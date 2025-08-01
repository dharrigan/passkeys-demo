(ns passkeys.demo.core.cache.impl
  {:author "David Harrigan"}
  (:require
   [clojure.tools.logging :as log]
   [taoensso.carmine :as car :refer [wcar]]))

(set! *warn-on-reflection* true)

(def ^:private two-weeks-in-seconds (* 60 60 24 14)) ;; 1209600

(defn redis-del
  [k connection]
  (log/tracef "Deleting key '%s' from redis." k)
  (when-let [result (wcar connection (car/del k))]
    (log/tracef "Deleted key '%s' from redis." k)
    result))

(defn redis-get
  [k connection]
  (log/tracef "Looking up key '%s' in redis." k)
  (when-let [result (wcar connection (car/get k))]
    (log/tracef "Found key '%s', value '%s' in redis." k result)
    result))

(defn redis-getdel
  [k connection]
  (log/tracef "Looking up key '%s' in redis and deleting if found." k)
  (when-let [result (wcar connection (car/getdel k))]
    (log/tracef "Found key '%s', value '%s' in redis. Key (and value) have also been deleted." k result)
    result))

(defn redis-keys
  [pattern connection]
  (log/tracef "Looking up keys using pattern '%s' in redis." pattern)
  (when-let [results (wcar connection (car/keys pattern))]
    (log/tracef "Found keys using pattern '%s', values '%s' in redis." pattern results)
    results))

(defn redis-put
  [k v {:keys [expiry-seconds] :or {expiry-seconds two-weeks-in-seconds} :as opts} connection]
  (wcar connection (car/set k v :ex expiry-seconds))
  (log/tracef "Stored key '%s', value '%s' into redis with an expiry of '%s' seconds." k v expiry-seconds)
  v)
