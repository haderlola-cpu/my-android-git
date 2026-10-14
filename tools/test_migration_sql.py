import re, json, sqlite3, random, uuid, copy
import sys
ROOT=sys.argv[1] if len(sys.argv)>1 else "/home/claude/work/elbeit-elaraby/app"
src=open(ROOT+"/src/main/java/com/example/data/AppDatabase.kt",encoding="utf-8").read()
block=src[src.index("MIGRATION_2_3_STATEMENTS: List<String> = listOf("):src.index("val MIGRATION_2_3: Migration")]
stmts=re.findall(r'"((?:[^"\\]|\\.)*)"',block)
assert len(stmts)==6, stmts
schema=json.load(open(ROOT+"/schemas/com.example.data.AppDatabase/2.json"))["database"]
ddl=[]
for e in schema["entities"]:
    ddl.append(e["createSql"].replace("${TABLE_NAME}",e["tableName"]))
    for i in e["indices"]: ddl.append(i["createSql"].replace("${TABLE_NAME}",e["tableName"]))

def make_v2(n_guests=20000):
    db=sqlite3.connect(":memory:"); db.isolation_level=None
    db.execute("PRAGMA foreign_keys=ON")
    for s in ddl: db.execute(s)
    ev_cols=[f["columnName"] for f in schema["entities"][0]["fields"]]
    gu_cols=[f["columnName"] for f in schema["entities"][1]["fields"]]
    rnd=random.Random(7)
    def ev(i):
        d={c:"x" for c in ev_cols}
        for c in ev_cols:
            t=[f for f in schema["entities"][0]["fields"] if f["columnName"]==c][0]["affinity"]
            d[c]={"INTEGER":0,"REAL":0.5,"TEXT":"t-"+c}[t]
        d.update(id=i,name="حفل %d"%i,date="2026-11-%02d"%i,backgroundUri=None,createdAt=1700000000000+i,showLocationQr=1)
        return d
    events=[ev(i) for i in (1,2,5,9)]  # non-contiguous ids
    for d in events:
        db.execute("INSERT INTO events(%s) VALUES(%s)"%(",".join(ev_cols),",".join("?"*len(ev_cols))),[d[c] for c in ev_cols])
    codes=set(); guests=[]
    for gid in range(1,n_guests+1):
        while True:
            c=str(rnd.randint(10_000_000,99_999_999))
            if c not in codes: codes.add(c); break
        att=rnd.random()<0.3
        guests.append(dict(id=gid*2,eventId=rnd.choice([1,2,5,9]),name="ضيف %d"%gid,normalizedName="ضيف %d"%gid,phone="0101%07d"%gid if gid%3 else "",
            normalizedPhone="",code=c,sequenceNumber=gid,attended=int(att),attendedAt=1700000100000+gid if att else None,
            whatsappSent=int(rnd.random()<0.5),createdAt=1700000000000+gid))
    for d in guests:
        db.execute("INSERT INTO guests(%s) VALUES(%s)"%(",".join(gu_cols),",".join("?"*len(gu_cols))),[d[c] for c in gu_cols])
    return db,ev_cols,gu_cols

def snapshot(db,table,cols):
    return {r[0]:r for r in db.execute("SELECT %s FROM %s ORDER BY id"%(",".join(cols),table))}

def run_migration(db):
    db.execute("BEGIN")          # Room runs migrations inside a transaction, foreign_keys stays ON
    for s in stmts: db.execute(s)
    db.execute("COMMIT")

V4=re.compile(r"^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
ok=[]
def check(name,cond):
    ok.append((name,bool(cond))); print(("PASS " if cond else "FAIL ")+name)

db,evc,guc=make_v2()
before_e=snapshot(db,"events",evc); before_g=snapshot(db,"guests",guc)
n_e,n_g=len(before_e),len(before_g)
run_migration(db)
after_e=snapshot(db,"events",evc); after_g=snapshot(db,"guests",guc)
check("event count preserved (%d)"%n_e, len(after_e)==n_e)
check("guest count preserved (%d)"%n_g, len(after_g)==n_g)
check("all original event columns unchanged (ids, fields)", before_e==after_e)
check("all original guest columns unchanged (ids, codes, attendance, whatsapp, eventId)", before_g==after_g)
ek=[r[0] for r in db.execute("SELECT cloudKey FROM events")]; gk=[r[0] for r in db.execute("SELECT cloudKey FROM guests")]
check("every event has non-empty cloudKey", all(ek))
check("every guest has non-empty cloudKey", all(gk))
check("all cloudKeys are valid UUID v4 (lowercase)", all(V4.match(k) for k in ek+gk))
check("event cloudKeys unique", len(set(ek))==len(ek))
check("guest cloudKeys unique (%d)"%len(gk), len(set(gk))==len(gk))
check("no cloudKey shared between events and guests", not (set(ek)&set(gk)))
check("foreign_key_check clean", db.execute("PRAGMA foreign_key_check").fetchall()==[])
check("guest->event relations intact (join count == guests)", db.execute("SELECT COUNT(*) FROM guests g JOIN events e ON e.id=g.eventId").fetchone()[0]==n_g)
per_before={}
for r in before_g.values(): per_before[r[guc.index('eventId')]]=per_before.get(r[guc.index('eventId')],0)+1
per_after=dict(db.execute("SELECT eventId,COUNT(*) FROM guests GROUP BY eventId").fetchall())
check("guests per event unchanged", per_before==per_after)
idx={r[1]:r[2] for r in db.execute("PRAGMA index_list(guests)")}
idxe={r[1]:r[2] for r in db.execute("PRAGMA index_list(events)")}
check("guests: unique code index kept + unique cloudKey index + eventId index", idx.get("index_guests_code")==1 and idx.get("index_guests_cloudKey")==1 and "index_guests_eventId" in idx)
check("events: unique cloudKey index", idxe.get("index_events_cloudKey")==1)
ti={r[1]:r for r in db.execute("PRAGMA table_info(events)")}
check("events.cloudKey is TEXT NOT NULL DEFAULT ''", ti["cloudKey"][2]=="TEXT" and ti["cloudKey"][3]==1 and ti["cloudKey"][4]=="''")
ti={r[1]:r for r in db.execute("PRAGMA table_info(guests)")}
check("guests.cloudKey is TEXT NOT NULL DEFAULT ''", ti["cloudKey"][2]=="TEXT" and ti["cloudKey"][3]==1 and ti["cloudKey"][4]=="''")
fk=db.execute("PRAGMA foreign_key_list(guests)").fetchall()
check("FK guests.eventId -> events.id ON DELETE CASCADE kept", len(fk)==1 and fk[0][2]=="events" and fk[0][6]=="CASCADE")
# idempotent backfill: re-running only the UPDATEs must not change existing keys
upd=[s for s in stmts if s.startswith("UPDATE")]
for s in upd: db.execute(s)
check("re-running backfill UPDATEs does not regenerate keys", [r[0] for r in db.execute("SELECT cloudKey FROM events")]==ek and [r[0] for r in db.execute("SELECT cloudKey FROM guests")]==gk)
# unique constraints enforced
try: db.execute("UPDATE guests SET cloudKey=? WHERE id=4",(gk[0],)); dup=False
except sqlite3.IntegrityError: dup=True
check("duplicate guest cloudKey rejected", dup)
try: db.execute("UPDATE guests SET code=(SELECT code FROM guests WHERE id=2) WHERE id=4"); dup=False
except sqlite3.IntegrityError: dup=True
check("duplicate guest code still rejected", dup)
# cascade still behaves: deleting one event removes only its guests
db.execute("DELETE FROM events WHERE id=2")
check("CASCADE still limited to the deleted event's guests", db.execute("SELECT COUNT(*) FROM guests").fetchone()[0]==n_g-per_before[2])
# partial-run safety: statements are inside one transaction -> a failure rolls back completely
db2,_,_=make_v2(500)
db2.execute("BEGIN")
try:
    for s in stmts[:3]: db2.execute(s)
    db2.execute("THIS IS NOT SQL")
except sqlite3.OperationalError:
    db2.execute("ROLLBACK")
cols=[r[1] for r in db2.execute("PRAGMA table_info(events)")]
check("failed migration rolls back (no cloudKey column, data intact)", "cloudKey" not in cols and db2.execute("SELECT COUNT(*) FROM events").fetchone()[0]==4)
# empty database migrates
db3=sqlite3.connect(":memory:"); db3.isolation_level=None; db3.execute("PRAGMA foreign_keys=ON")
for s in ddl: db3.execute(s)
run_migration(db3)
check("empty v2 database migrates", db3.execute("SELECT COUNT(*) FROM events").fetchone()[0]==0)
print("\nSQLite", sqlite3.sqlite_version, "| %d/%d checks passed"%(sum(1 for _,c in ok if c),len(ok)))
raise SystemExit(0 if all(c for _,c in ok) else 1)
