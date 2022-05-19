package fr.paris.lutece.plugins.stock.modules.solr.service;

import java.util.List;

import fr.paris.lutece.plugins.search.solr.business.indexeraction.SolrIndexerAction;
import fr.paris.lutece.plugins.search.solr.business.indexeraction.SolrIndexerActionHome;
import fr.paris.lutece.plugins.search.solr.service.SolrPlugin;
import fr.paris.lutece.portal.service.plugin.PluginService;

public class StockSolrService
{
    public static boolean isSolrIndexerActionExists( int nId, int nIdTask, String strTypeResource )
    {
        List<SolrIndexerAction> actions = SolrIndexerActionHome.getList( PluginService.getPlugin( SolrPlugin.PLUGIN_NAME ) );
        return isActionExists( nId, nIdTask, strTypeResource, actions );
    }

    public static boolean isActionExists( int nId, int nIdTask, String strTypeResource, List<SolrIndexerAction> actions )
    {
        for ( SolrIndexerAction action : actions )
        {
            if ( Integer.toString( nId ).equals( action.getIdDocument( ) ) && strTypeResource.equals( action.getTypeResource( ) ) && nIdTask == action.getIdTask( ) )
            {
                return true;
            }
        }
        return false;
    }
}
