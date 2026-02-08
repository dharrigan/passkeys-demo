(ns passkeys.demo.components.database
  {:author "David Harrigan"}
  (:require
   [next.jdbc.connection :as connection]
   [passkeys.demo.core.database.migration :as migration])
  (:import
   [com.zaxxer.hikari HikariDataSource]))

(set! *warn-on-reflection* true)

;; DONUT LIFECYCLE FUNCTIONS ↓

(defn start ^HikariDataSource
  [{:keys [db additional-config] :as config}]
  (let [db-spec (merge db additional-config)]
    (connection/->pool HikariDataSource db-spec)))

(defn post-start
  [^HikariDataSource datasource {:keys [additional-config] :as config}]
  (let [{:keys [migration-locations]} additional-config]
    (migration/migrate datasource migration-locations)))

(defn stop
  [^HikariDataSource datasource]
  (.close datasource))
