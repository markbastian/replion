(ns replion.core
  (:require [datomic.client.api :as d]
            [nrepl.server :refer [start-server]]
            [replion.spiderman-db :as spiderman]))

(defonce server (start-server :bind "0.0.0.0" :port 3001))

;Note that your region may be different and your system name will surely be different.
(def config
  {:server-type :ion
   :region      "us-east-1"
   :system      "repl-ion"
   :endpoint    "http://entry.repl-ion.us-east-1.datomic.net:8182/"
   :proxy-port  8182})

(def client
  (memoize (fn [] (d/client config))))

(defn setup [client]
  (if (not-any? #{"parker"} (d/list-databases client {}))
    (let [_ (d/create-database client {:db-name "parker"})
          conn (spiderman/transact-all (d/connect client {:db-name "parker"}))]
      conn)
    (d/connect client {:db-name "parker"})))

(defn connection []
  (setup (client)))