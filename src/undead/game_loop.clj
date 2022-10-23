(ns undead.game-loop
  (:require [clojure.core.async :refer [put!]]
            [undead.actionizer :as actionizer]
            [undead.game :as game]))

(defn start! [ws-channel]
  (put! ws-channel
        (try
          (mapcat actionizer/event->actions (game/get-initial-events (System/currentTimeMillis)))
          (catch Exception e
            [[:assoc-in [:error] (pr-str e)]]))))
