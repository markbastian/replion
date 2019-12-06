(ns replion.web
  (:require [cheshire.core :as ch]
            [datomic.ion.lambda.api-gateway :as apigw]))

(defn handler
  [request]
  (ch/encode
    {:status 200
     :body   "OK"}))

(def handler-proxy
  (apigw/ionize handler))