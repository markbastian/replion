(ns replion.spiderman-lambdas
  (:require [datomic.client.api :as d]
            [replion.spiderman-db :as spiderman]
            [replion.core :as core]))

(defn parker-status
  [{:keys [date]}]
  (let [db (d/db (core/connection))]
    (spiderman/parker-status-query db)))