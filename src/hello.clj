(ns hello
  (:import [javax.swing JFrame JLabel]))

(defn -main []
  (let [frame (JFrame. "Hello LCD")
        label (JLabel. "Hello, World!" JLabel/CENTER)]
    (.setSize frame 320 240)
    (.add frame label)
    (.setDefaultCloseOperation frame JFrame/EXIT_ON_CLOSE)
    (.setVisible frame true)))

(-main)

