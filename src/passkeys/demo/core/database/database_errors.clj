(ns passkeys.demo.core.database.database-errors
  {:author "David Harrigan"})

(set! *warn-on-reflection* true)

(defn generic-database-error
  [exception sql opts]
  {:message (ex-message exception) :data {:error :database.unavailable :sql sql :opts opts}})
