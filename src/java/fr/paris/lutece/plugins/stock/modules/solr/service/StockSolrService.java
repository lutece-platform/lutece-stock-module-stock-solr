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
package fr.paris.lutece.plugins.stock.modules.solr.service;

import java.util.List;

import fr.paris.lutece.plugins.search.solr.business.indexeraction.SolrIndexerAction;
import fr.paris.lutece.plugins.search.solr.business.indexeraction.SolrIndexerActionHome;
import fr.paris.lutece.plugins.search.solr.service.SolrPlugin;
import fr.paris.lutece.portal.service.plugin.PluginService;

public class StockSolrService
{
    /**
     * Check if the Object Task is present in the Indexer actions
     * 
     * @param nId
     *            the Object Id
     * @param nIdTask
     *            the Indexer task Id
     * @param strTypeResource
     *            the Indexer resource type
     * @return true if the Object is present, false otherwise
     */
    public static boolean isSolrIndexerActionExists( int nId, int nIdTask, String strTypeResource )
    {
        List<SolrIndexerAction> actions = SolrIndexerActionHome.getList( PluginService.getPlugin( SolrPlugin.PLUGIN_NAME ) );
        return isActionExists( nId, nIdTask, strTypeResource, actions );
    }

    /**
     * Search for the Object Id - Task in the Indexer task
     * 
     * @param nId
     *            the Object Id
     * @param nIdTask
     *            the Indexer task Id
     * @param strTypeResource
     *            the Indexer resource type
     * @param actions
     *            the list of Indexer actions
     * @return true if the Object is present, false otherwise
     */
    public static boolean isActionExists( int nId, int nIdTask, String strTypeResource, List<SolrIndexerAction> actions )
    {
        for ( SolrIndexerAction action : actions )
        {
            if ( Integer.toString( nId ).equals( action.getIdDocument( ) ) && strTypeResource.equals( action.getTypeResource( ) )
                    && nIdTask == action.getIdTask( ) )
            {
                return true;
            }
        }
        return false;
    }
}
