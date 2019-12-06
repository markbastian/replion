(ns replion.spiderman-lambdas
  (:require [datomic.client.api :as d]
            [replion.spiderman-db :as spiderman]
            [replion.core :as core]
            [clojure.pprint :as pp]
            [clojure.string :as cs]
            [cheshire.core :as ch])
  (:import (java.text SimpleDateFormat)))

(defn parker-status-orig
  [{:keys [date]}]
  (let [db (d/db (core/connection))]
    (spiderman/parker-status-query db)))

(defn parker-status-fixed-output
  [{:keys [date]}]
  (let [db (d/db (core/connection))
        [status as-of-date] (first (spiderman/parker-status-query db))]
    (format "{\"%s\": \"%s\"}" (name status) as-of-date)))

(defn parker-status-broken
  [{:keys [date]}]
  (let [db (d/db (core/connection))
        as-of-db (d/as-of db date)
        [status as-of-date] (first (spiderman/parker-status-query as-of-db))]
    (format
      "{\"%s\": \"%s\"}"
      (name status)
      as-of-date)))

(defn parker-status-input-dump
  [args]
  (let [db (d/db (core/connection))
        ;as-of-db (d/as-of db date)
        [status as-of-date] (first (spiderman/parker-status-query db))]
    (format
      "{\"args\":\"%s\",\n\"%s\": \"%s\"}"
      (with-out-str (pp/pprint args))
      (name status)
      as-of-date)))

(defn parker-status-pre-cheshire
  [{:keys [input]}]
  (let [date (.parse (SimpleDateFormat. "yyyy-MM-dd") (cs/replace input #"\"" ""))
        db (d/db (core/connection))
        as-of-db (d/as-of db date)
        [status as-of-date] (first (spiderman/parker-status-query as-of-db))]
    (format
      "{\"%s\": \"%s\"}"
      (name status)
      as-of-date)))

(defn parker-status
  [{:keys [input]}]
  (let [date (.parse (SimpleDateFormat. "yyyy-MM-dd") (ch/parse-string input))
        db (d/db (core/connection))
        as-of-db (d/as-of db date)
        [status as-of-date] (first (spiderman/parker-status-query as-of-db))]
    (ch/encode
      {status as-of-date})))