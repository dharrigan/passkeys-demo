(ns passkeys.demo.ui.dashboard.render
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.core.security.model.principal :as principal]
   [passkeys.demo.ui.utils.thymeleaf :as thymeleaf]))

(set! *warn-on-reflection* true)

(defn index
  [{{:keys [account]} :identity :as request}]
  (let [logged-in-as (str (principal/first-name account) " " (principal/last-name account))
        email (principal/email account)]
    (thymeleaf/html "dashboard/main/index" request {:logged-in-as logged-in-as :email email})))
