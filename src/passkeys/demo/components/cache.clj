(ns passkeys.demo.components.cache
  {:author "David Harrigan"}
  (:require
   [taoensso.carmine :as car])
  (:import
   [taoensso.carmine.connections ConnectionPool]))

(set! *warn-on-reflection* true)

;; DONUT LIFECYCLE FUNCTIONS ↓

(defn start
  [{:keys [uri ttl-seconds] :as config}]
  {:connection {:pool (car/connection-pool {}) :spec {:uri uri}} :ttl-seconds ttl-seconds})

(defn stop
  [{{:keys [pool]} :connection :as instance}]
  (.close ^ConnectionPool pool))
