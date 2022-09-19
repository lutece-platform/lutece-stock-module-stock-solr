package fr.paris.lutece.plugins.stock.modules.solr.service.daemon;

import java.sql.Timestamp;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import fr.paris.lutece.plugins.search.solr.business.indexeraction.SolrIndexerAction;
import fr.paris.lutece.plugins.search.solr.business.indexeraction.SolrIndexerActionHome;
import fr.paris.lutece.plugins.search.solr.service.SolrPlugin;
import fr.paris.lutece.plugins.stock.modules.solr.service.StockSolrService;
import fr.paris.lutece.plugins.stock.service.IProductService;
import fr.paris.lutece.portal.business.indexeraction.IndexerAction;
import fr.paris.lutece.portal.service.daemon.Daemon;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;

public class StockSolrInjectProductsTaskDaemon extends Daemon
{
    @Inject
    @Named( "stock.productService" )
    IProductService             _productService;

    private static final String TYPE_RESOURCE = "DOCUMENT_STOCK";

    private static final String PROPRETY_STOCK_SOLR_ELAPSED_TIME = "stock-solr.daemon.inject.products.task.elapsed.time";

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void run( )
    {
        int nMinutesAgo = AppPropertiesService.getPropertyInt( PROPRETY_STOCK_SOLR_ELAPSED_TIME, 60 );
        Timestamp timestampStart = new Timestamp( System.currentTimeMillis( ) );
        Timestamp timestampEnd = new Timestamp( System.currentTimeMillis( ) - ( nMinutesAgo * 60 * 1000 ) );
        if ( null == _productService )
        {
            initializeProductService( );
        }
        List<Integer> listProductId = _productService.getProductsForTaskTimed( "date", timestampStart, timestampEnd );
        if ( ( listProductId != null ) && !listProductId.isEmpty( ) )
        {
            listProductId.forEach( nId -> addProductTask( nId, IndexerAction.TASK_MODIFY ) );
        }
    }

    private void initializeProductService() {
        List<IProductService> productServices = SpringContextService.getBeansOfType( IProductService.class );
        if ( ( productServices != null ) && ( productServices.size( ) > 0 ) )
        {
            _productService = productServices.get( 0 );
        }
    }

    private void addProductTask( Integer nProductId, int nTask )
    {
        if( !StockSolrService.isSolrIndexerActionExists(nProductId, nTask, TYPE_RESOURCE ) ) {
            SolrIndexerAction indexerAction = new SolrIndexerAction( );
            indexerAction.setIdDocument( nProductId.toString( ) );
            indexerAction.setIdTask( nTask );
            indexerAction.setTypeResource( TYPE_RESOURCE );
            SolrIndexerActionHome.create( indexerAction, PluginService.getPlugin( SolrPlugin.PLUGIN_NAME ) );
        }
    }
}
