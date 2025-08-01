(ns passkeys.demo.api.general.routes
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.api.general.handler :as handler]))

(set! *warn-on-reflection* true)

(defn routes
  []
  ["/api"
   ["/ping" {:get {:handler handler/ping}}]])
