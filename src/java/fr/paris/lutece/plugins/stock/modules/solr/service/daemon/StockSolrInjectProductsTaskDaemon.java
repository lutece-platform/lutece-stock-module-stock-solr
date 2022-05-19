/*
 * Copyright (c) 2002-2021, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.stock.modules.solr.service.daemon;

import java.sql.Timestamp;
import java.util.List;

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
    private static final String TYPE_RESOURCE = "DOCUMENT_STOCK";

    private static final String PROPRETY_STOCK_SOLR_ELAPSED_TIME = "stock-solr.daemon.inject.products.task.elapsed.time";

    private static final String KEY_DATE = "date";

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
        timestampEnd = new Timestamp( 122, 0, 1, 0, 0, 0, 0 );

        IProductService _productService = SpringContextService.getBean( "stock.productService" );
        List<Integer> listProductId = _productService.getProductsIdsForTaskTimed( KEY_DATE, timestampStart, timestampEnd );
        if ( ( listProductId != null ) && !listProductId.isEmpty( ) )
        {
            listProductId.forEach( nId -> addProductTask( nId, IndexerAction.TASK_MODIFY ) );
        }
    }

    /**
     * Add the Product Task to the Indexer actions
     * 
     * @param nProductId
     *            the Product Id
     * @param nTask
     *            the Indexer task Id
     */
    private void addProductTask( Integer nProductId, int nTask )
    {
        if ( !StockSolrService.isSolrIndexerActionExists( nProductId, nTask, TYPE_RESOURCE ) )
        {
            SolrIndexerAction indexerAction = new SolrIndexerAction( );
            indexerAction.setIdDocument( nProductId.toString( ) );
            indexerAction.setIdTask( nTask );
            indexerAction.setTypeResource( TYPE_RESOURCE );
            SolrIndexerActionHome.create( indexerAction, PluginService.getPlugin( SolrPlugin.PLUGIN_NAME ) );
        }
    }
}
