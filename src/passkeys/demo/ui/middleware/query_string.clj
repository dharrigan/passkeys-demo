(ns passkeys.demo.ui.middleware.query-string
  {:author "David Harrigan"}
  (:require
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as csk-extras]))

(set! *warn-on-reflection* true)

(defn ^:private with-query-string
  [handler]
  (fn [request]
    (-> (update-in request [:parameters :query] #(csk-extras/transform-keys csk/->kebab-case %))
        (handler))))

(def query-string-middleware
  {:name ::query-string
   :wrap with-query-string})
