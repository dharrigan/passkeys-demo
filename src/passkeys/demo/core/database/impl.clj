(ns passkeys.demo.core.database.impl
  {:author "David Harrigan"}
  (:require
   [clojure.tools.logging :as log]
   [next.jdbc :as jdbc]
   [next.jdbc.date-time] ;; https://github.com/seancorfield/next-jdbc/blob/a568f0fa87b4216941bfefbf3e86f2b182592ff7/src/next/jdbc/date_time.clj#L30
   [next.jdbc.result-set :refer [as-kebab-maps]]
   [passkeys.demo.core.database.database-errors :as database-errors :refer [generic-database-error]]
   [passkeys.demo.core.errors.impl :as errors :refer [throw-database-exception]]))

(set! *warn-on-reflection* true)

(next.jdbc.date-time/read-as-instant) ;; safest option.

(defn execute
  ([datasource sql] (execute datasource sql {}))
  ([datasource sql opts]
   (log/tracef "Executing JDBC '%s'." sql)
   (try
     (when-let [results (jdbc/execute-one! datasource sql (merge {:builder-fn as-kebab-maps} opts))]
       (log/tracef "JDBC result '%s'." results)
       results)
     (catch Exception exception
       (log/error exception)
       (throw-database-exception (generic-database-error exception sql opts))))))

(defn select
  ([datasource sql] (select datasource sql {}))
  ([datasource sql opts]
   (log/tracef "Executing JDBC '%s'." sql)
   (try
     (when-let [result (jdbc/execute-one! datasource sql (merge {:builder-fn as-kebab-maps} opts))]
       (log/tracef "JDBC result '%s'." result)
       result)
     (catch Exception exception
       (log/error exception)
       (throw-database-exception (generic-database-error exception sql opts))))))

(defn select-many
  ([datasource sql] (select-many datasource sql {}))
  ([datasource sql opts]
   (log/tracef "Executing JDBC '%s'." sql)
   (try
     (when-let [results (jdbc/execute! datasource sql (merge {:builder-fn as-kebab-maps} opts))]
       (log/tracef "JDBC result '%s'." results)
       results)
     (catch Exception exception
       (log/error exception)
       (throw-database-exception (generic-database-error exception sql opts))))))
