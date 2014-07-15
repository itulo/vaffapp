#!/bin/sh
DB_NAME=vaffapp.db
if [ -e $DB_NAME ]
then
  rm $DB_NAME
fi
sqlite3 vaffapp.db < create_db.sql
