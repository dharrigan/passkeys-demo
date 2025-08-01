(ns passkeys.demo.ui.middleware.transactions
  {:author "David Harrigan"}
  (:require
   [next.jdbc :as jdbc]))

(set! *warn-on-reflection* true)

(defn ^:private with-transactions
  [handler]
  (fn [{{{:keys [passkeys-demo-db]} :components} :system :as request}]
    (jdbc/with-transaction [tx passkeys-demo-db]
      (handler (assoc-in request [:system :components :passkeys-demo-db] tx)))))

(def transactions-middleware
  {:name ::transactions
   :description "Creates a new database transaction for each request/response."
   :wrap with-transactions})
