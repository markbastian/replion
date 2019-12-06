(ns replion.spiderman-db
  (:require [datomic.client.api :as d]))

(def schema
  [{:db/id "datomic.tx" :db/txInstant #inst "1990-01-01"}
   {:db/ident       :name
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :gender
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :status
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}

   {:db/ident       :alias
    :db/valueType   :db.type/string
    :db/unique      :db.unique/value
    :db/cardinality :db.cardinality/many}

   {:db/ident       :spouse
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :child
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident       :girlfriend
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}])

(def initial-data
  [{:db/id "datomic.tx" :db/txInstant #inst "2000-01-01"}
   {:name   "Peter Parker"
    :gender "M"
    :status :kid}
   {:name "Richard Parker" :gender "M" :spouse {:name "Mary Parker"}}
   {:name "Mary Parker" :gender "F" :spouse {:name "Richard Parker"}}
   {:name "Ben Parker" :gender "M" :spouse {:name "May Parker"}}
   {:name "May Parker" :gender "F" :spouse {:name "Ben Parker"}}
   {:name "Richard Parker" :child {:name "Peter Parker"}}
   {:child {:name "Peter Parker"}}
   {:child [{:name "Ben Parker"} {:name "Richard Parker"}]}])

(def spider-bite-data
  [{:db/id "datomic.tx" :db/txInstant #inst "2001-01-01"}
   {:name   "Peter Parker"
    :status :bitten}])

(def hero-moment-data
  [{:db/id "datomic.tx" :db/txInstant #inst "2001-01-05"}
   {:name   "Peter Parker"
    :status :spider-man
    :alias  ["Spider-Man" "Spidey"]}
   {:name   "Ben Parker"
    :status :deceased}])

(def girlfriend-1-data
  [{:db/id "datomic.tx" :db/txInstant #inst "2001-01-11"}
   {:name       "Peter Parker"
    :girlfriend {:name "Gwen Stacey"}}])

(def girlfriend-2-data
  [{:db/id "datomic.tx" :db/txInstant #inst "2002-01-01"}
   {:name       "Peter Parker"
    :girlfriend {:name "Mary Jane Watson" :alias "MJ Watson"}}])

(defn transact-all [conn]
  (doto conn
    (d/transact {:tx-data schema})
    (d/transact {:tx-data initial-data})
    (d/transact {:tx-data spider-bite-data})
    (d/transact {:tx-data hero-moment-data})
    (d/transact {:tx-data girlfriend-1-data})
    (d/transact {:tx-data girlfriend-2-data})))

(def gf-query
  '[:find ?gf-name
    :where
    [?e :name "Peter Parker"]
    [?e :girlfriend ?gf]
    [?gf :name ?gf-name]])

(defn get-current-girlfriend [db]
  (d/q
    gf-query
    db))

(defn get-all-girlfriends [db]
  (d/q
    gf-query
    (d/history db))
  )

(defn girlfriends-before-bite [db]
  (d/q
    gf-query
    (d/as-of db #inst "2000-01-01")))

(defn girlfriends-before-mj [db]
  (d/q
    gf-query
    (d/as-of db #inst "2001-05-01")))

;Examine the status times
(def status-query
  '[:find ?status ?time
    :where
    [?e :name "Peter Parker"]
    [?e :status ?status ?t true]
    [?t :db/txInstant ?time]])

(defn parker-status-query [db]
  (d/q status-query db))

(defn historical-statuses [db]
  (let [current-db db
        full-history-db (d/history current-db)
        snapshot-db (d/as-of current-db #inst "2001-01-02")
        history-at-t-db (d/history snapshot-db)
        f (fn [db] (vec (sort-by second (d/q status-query db))))]
    {:current-status         (f current-db)
     :status-before-ben-dies (f snapshot-db)
     :full-history-of-status (f full-history-db)
     :history-until-ben-dies (f history-at-t-db)}))