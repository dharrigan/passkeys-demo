(ns passkeys.demo.core.session.impl
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.core.session.repository :as repository]))

(set! *warn-on-reflection* true)

(defn delete-session
  [session-id system]
  (repository/delete-session session-id system))

(defn get-session
  [session-id system]
  (repository/get-session session-id system))

(defn save-session
  [data system]
  (repository/save-session data system))
