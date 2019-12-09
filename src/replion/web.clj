(ns replion.web
  (:require [datomic.ion.lambda.api-gateway :as apigw]
            [clojure.pprint :as pp]
            [ring.util.http-response :refer [ok not-found]]
            [reitit.coercion.spec]
            [reitit.ring :as ring]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [muuntaja.middleware :as middleware]
            [ring.middleware.params :as params])
  (:import (java.util Date)))

(defn handler-original
  [request]
  {:status  200
   :headers {}
   :body    "OK"})

(defn handler-request-dump
  [request]
  {:status  200
   :headers {}
   :body    (with-out-str (pp/pprint request))})

(defn runtime-data []
  (let [rt (Runtime/getRuntime) mb (* 1024.0 1024.0)]
    {:free-memory-MB  (/ (.freeMemory rt) mb)
     :max-memory-MB   (/ (.maxMemory rt) mb)
     :total-memory-MB (/ (.totalMemory rt) mb)}))

(def router
  (ring/router
    [["/api" {:swagger {:tags    ["basic api"]
                        :summary "This is the api"}}
      ["/time" {:get (fn [_] (ok {:time (str (Date.))}))}]
      ["/memory" {:get (fn [_] (ok {:runtime-stats (runtime-data)}))}]]
     ["" {:no-doc true
          :swagger {:basePath "/dev"}}
      ["/swagger.json" {:get (swagger/create-swagger-handler)}]
      ["/api-docs*" {:get (swagger-ui/create-swagger-ui-handler {:url "/dev/swagger.json"})}]]]
    {:data {:coercion   reitit.coercion.spec/coercion
            :middleware [params/wrap-params
                         middleware/wrap-format]}}))

(def handler
  (ring/ring-handler
    router
    (constantly (not-found "Not found"))))

(def handler-proxy
  (apigw/ionize handler))