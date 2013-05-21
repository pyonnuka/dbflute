cd ..
ant -f build.xml reflect-to-basic
ant -f build.xml reflect-to-spring
ant -f build.xml reflect-to-guice
ant -f build.xml reflect-to-mysql
ant -f build.xml reflect-to-postgresql
ant -f build.xml reflect-to-saflute
export answer=y

cd ../dbflute-basic-example
mvn -e eclipath:sync eclipath:clean
cd dbflute_exampledb
rm ./log/*.log
. jdbc.sh
. doc.sh
. generate.sh
. sql2entity.sh

cd ../../dbflute-spring-example
mvn -e eclipath:sync eclipath:clean
cd dbflute_exampledb
rm ./log/*.log
. jdbc.sh
. doc.sh
. generate.sh
. sql2entity.sh

cd ../../dbflute-guice-example/dbflute_exampledb
rm ./log/*.log
. jdbc.sh
. doc.sh
. generate.sh
. sql2entity.sh
. manage.sh load-data-reverse
. manage.sh schema-sync-check
. manage.sh freegen
. diffworld-test.sh

cd ../../dbflute-mysql-example/dbflute_exampledb
rm ./log/*.log
. replace-schema.sh
. jdbc.sh
. doc.sh
. generate.sh
. sql2entity.sh
. outside-sql-test.sh
. manage.sh load-data-reverse
. manage.sh freegen

cd ../../dbflute-postgresql-example/dbflute_exampledb
rm ./log/*.log
. jdbc.sh
. doc.sh
. generate.sh
. sql2entity.sh
. outside-sql-test.sh
. manage.sh load-data-reverse

cd ../../dbflute-saflute-example/dbflute_exampledb
rm ./log/*.log
. replace-schema.sh
. manage.sh load-data-reverse
. replace-schema.sh
. jdbc.sh
. doc.sh
. generate.sh
. sql2entity.sh
. outside-sql-test.sh
. manage.sh freegen
