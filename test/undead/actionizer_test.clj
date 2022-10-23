(ns undead.actionizer-test
  (:require [clojure.test :refer [deftest is testing]]
            [undead.actionizer :as sut]))

(deftest actionize--add-zombie
  (is (= (sut/event->actions
          [:add-zombie {:id :zombie-1
                        :kind :biker
                        :health {:max 4 :current 4}}])
         [[:assoc-in [:zombies :zombie-1]
           {:kind :biker
            :hearts [[:heart :heart :heart :heart]]}]]))

  (is (= (sut/event->actions
          [:add-zombie {:id :zombie-1
                        :kind :biker
                        :health {:max 4 :current 3}}])
         [[:assoc-in [:zombies :zombie-1]
           {:kind :biker
            :hearts [[:heart :heart :heart :lost]]}]]))

  (is (= (sut/event->actions
          [:add-zombie {:id :zombie-1
                        :kind :biker
                        :health {:max 9 :current 8}}])
         [[:assoc-in [:zombies :zombie-1]
           {:kind :biker
            :hearts [[:heart :heart :heart :heart :heart]
                     [:heart :heart :heart :lost]]}]])))

(deftest actionize--set-player-health
  (is (= (sut/event->actions
          [:set-player-health {:max 3, :current 3}])
         [[:assoc-in [:player :hearts] [:heart :heart :heart]]]))

  (is (= (sut/event->actions
          [:set-player-health {:max 3, :current 2}])
         [[:assoc-in [:player :hearts] [:heart :heart :lost]]])))