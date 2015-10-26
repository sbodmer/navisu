/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bzh.terrevirtuelle.navisu.domain.navigation.app;

import bzh.terrevirtuelle.navisu.domain.navigation.NavigationDataSet;
import bzh.terrevirtuelle.navisu.domain.ship.model.Ship;
import bzh.terrevirtuelle.navisu.domain.ship.model.ShipBuilder;
import bzh.terrevirtuelle.navisu.util.xml.ImportExportXML;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;

/**
 * NaVisu
 *
 * @date 26 oct. 2015
 * @author Serge Morvan
 */
public class AppMain {

    public static void main(String[] args) {
        Ship ship = ShipBuilder
                .create()
                .mmsi(124578)
                .name("Lithops")
                .build();
        NavigationDataSet navigationDataSet = new NavigationDataSet();
        navigationDataSet.add(ship);
        NavigationDataSet nds = null;
        try {
            ImportExportXML.exports(navigationDataSet, "data/test.xml");
            nds = ImportExportXML.imports(navigationDataSet, new File("data/test.xml"));
        } catch (JAXBException | FileNotFoundException ex) {
            Logger.getLogger(AppMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("s : " + nds);
    }
}
