import json
E=[("id","INTEGER",1),("name","TEXT",1),("date","TEXT",1),("timeFrom","TEXT",1),("timeTo","TEXT",1),
("backgroundType","TEXT",1),("backgroundPresetId","INTEGER",1),("backgroundUri","TEXT",0),("backgroundAspectRatio","REAL",1),
("nameColorHex","TEXT",1),("qrColorHex","TEXT",1),("qrBgColorHex","TEXT",1),("fontIndex","INTEGER",1),
("showGuestName","INTEGER",1),("showDate","INTEGER",1),("showTime","INTEGER",1),("qrSizeRatio","REAL",1),("nameSizeSp","REAL",1),
("qrPosX","REAL",1),("qrPosY","REAL",1),("namePosX","REAL",1),("namePosY","REAL",1),("datePosX","REAL",1),("datePosY","REAL",1),
("whatsappTemplate","TEXT",1),("createdAt","INTEGER",1),("groomName","TEXT",1),("brideName","TEXT",1),("headerVerse","TEXT",1),
("hostsText","TEXT",1),("dayName","TEXT",1),("venueName","TEXT",1),("venueAddress","TEXT",1),("locationUrl","TEXT",1),
("showLocationQr","INTEGER",1),("notesText","TEXT",1),("closingText","TEXT",1)]
G=[("id","INTEGER",1),("eventId","INTEGER",1),("name","TEXT",1),("normalizedName","TEXT",1),("phone","TEXT",1),("normalizedPhone","TEXT",1),
("code","TEXT",1),("sequenceNumber","INTEGER",1),("attended","INTEGER",1),("attendedAt","INTEGER",0),("whatsappSent","INTEGER",1),("createdAt","INTEGER",1)]
def cols(F): return ", ".join("`%s` %s%s"%(n,t," NOT NULL" if nn else "") if n!="id" else "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL" for n,t,nn in F)
ev_sql="CREATE TABLE IF NOT EXISTS `%s` ("+cols(E)+")"
gu_sql="CREATE TABLE IF NOT EXISTS `%s` ("+cols(G)+", FOREIGN KEY(`eventId`) REFERENCES `events`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
def ent(tbl,F,sql,idx,fk):
    return {"tableName":tbl,"createSql":sql.replace("%s","${TABLE_NAME}"),
     "fields":[{"fieldPath":n,"columnName":n,"affinity":t,"notNull":bool(nn)} for n,t,nn in F],
     "primaryKey":{"autoGenerate":True,"columnNames":["id"]},"indices":idx,"foreignKeys":fk}
gidx=[{"name":"index_guests_eventId","unique":False,"columnNames":["eventId"],"orders":[],"createSql":"CREATE INDEX IF NOT EXISTS `index_guests_eventId` ON `${TABLE_NAME}` (`eventId`)"},
      {"name":"index_guests_code","unique":True,"columnNames":["code"],"orders":[],"createSql":"CREATE UNIQUE INDEX IF NOT EXISTS `index_guests_code` ON `${TABLE_NAME}` (`code`)"}]
fk=[{"table":"events","onDelete":"CASCADE","onUpdate":"NO ACTION","columns":["eventId"],"referencedColumns":["id"]}]
HASH="reconstructed-v2-schema-not-room-generated"
doc={"formatVersion":1,"database":{"version":2,"identityHash":HASH,
 "entities":[ent("events",E,ev_sql,[],[]),ent("guests",G,gu_sql,gidx,fk)],"views":[],
 "setupQueries":["CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
 "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '%s')"%HASH]}}
json.dump(doc,open("/home/claude/work/elbeit-elaraby/app/schemas/com.example.data.AppDatabase/2.json","w"),indent=2)
