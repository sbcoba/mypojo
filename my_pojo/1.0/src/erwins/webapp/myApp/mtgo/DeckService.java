
package erwins.webapp.myApp.mtgo;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import erwins.util.webapp.GenericAppEngineDao;
import erwins.util.webapp.GenericAppEngineService;

@Service
@Transactional(readOnly = true)
public class DeckService extends GenericAppEngineService<Deck>{
    
	@Autowired private DeckDao deckDao;
    
    public Collection<Deck> findAll(){
    	return deckDao.findAll();
    }
    
    @Transactional
    public Deck updateWinRate(String id,boolean isWin,boolean isMinus){
    	Deck server = deckDao.getById(id);
    	int count = isMinus ? -1 : 1;
    	if(isWin) server.setWin(server.getWin()+count);
    	else server.setLose(server.getLose()+count);
    	return server;
    }

	@Override
	protected GenericAppEngineDao<Deck> getDao() {
		return deckDao;
	}

}
