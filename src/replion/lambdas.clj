(ns replion.lambdas
  (:require [replion.core :as core]
            [clojure.pprint :as pp]))

(defn hello [_] "{\"hello\":\"world\"}")

(defn hello-new [args]
  (format
    "{\"args\":%s}"
    (with-out-str (pp/pprint args))))
