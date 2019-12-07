(ns replion.web
  (:require [datomic.ion.lambda.api-gateway :as apigw]
            [clojure.pprint :as pp]
            [reitit.ring :as ring]
            [reitit.swagger :as swagger]
            ;[reitit.swagger-ui :as swagger-ui]
            ))

(defn handler-original
  [request]
  {:status  200
   :headers {}
   :body    "OK"})

(defn handler
  [request]
  {:status  200
   :headers {}
   :body    (with-out-str (pp/pprint request))})

(def handler-proxy
  (apigw/ionize handler))