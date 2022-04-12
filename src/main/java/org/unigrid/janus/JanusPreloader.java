/*
    The Janus Wallet
    Copyright © 2021 The Unigrid Foundation

    This program is free software: you can redistribute it and/or modify it under the terms of the
    addended GNU Affero General Public License as published by the Free Software Foundation, version 3
    of the License (see COPYING and COPYING.addendum).

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received an addended copy of the GNU Affero General Public License with this program.
    If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Preloader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.unigrid.janus.model.service.Daemon;
import org.unigrid.janus.view.SplashScreen;

/**
 *
 * @author marcus
 */
@ApplicationScoped
public class JanusPreloader extends Preloader {
        
    @Inject
    private Daemon daemon;
    
    @Inject
    private SplashScreen splashScreen;
    
    public JanusPreloader(){
	
    }
    
    public void init() throws Exception{
        daemonStart();	
	
    }
    
    public void start(Stage primaryStage) throws Exception{
        
	splashScreen.show();
    }
    
    public void handelApplicationNotification(PreloaderNotification info){
        
    }
    
    public void handelStadeChangeNotification(StateChangeNotification info){
        
	StateChangeNotification.Type type = info.getType();
	switch (type){
	    case BEFORE_START:
		splashScreen.hide();
		break;
	}
	
    }
    
    private void daemonStart(){
        try {
            daemon.start();
	} catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR,
                    e.getMessage(),
                    ButtonType.OK);
                    a.showAndWait();
	}
    }
    
    private void daemonStop(){	
	try {
	    daemon.stop();
	} catch (InterruptedException ex) {
	    //TODO: Fix how the exception is handeld
	    Logger.getLogger(JanusPreloader.class.getName()).log(Level.SEVERE, null, ex);
	}
    }
}
