/*
 * Copyright (c) 2002-2014, Mairie de Paris
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
package fr.paris.lutece.plugins.stock.modules.solr.indexer;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import fr.paris.lutece.plugins.search.solr.business.field.Field;
import fr.paris.lutece.plugins.search.solr.indexer.SolrIndexer;
import fr.paris.lutece.plugins.search.solr.indexer.SolrIndexerService;
import fr.paris.lutece.plugins.search.solr.indexer.SolrItem;
import fr.paris.lutece.plugins.search.solr.util.SolrConstants;
import fr.paris.lutece.plugins.stock.business.attribute.offer.OfferAttributeDate;
import fr.paris.lutece.plugins.stock.business.attribute.product.ProductAttribute;
import fr.paris.lutece.plugins.stock.business.attribute.product.ProductAttributeDate;
import fr.paris.lutece.plugins.stock.business.attribute.product.ProductAttributeNum;
import fr.paris.lutece.plugins.stock.business.attribute.provider.ProviderAttribute;
import fr.paris.lutece.plugins.stock.business.attribute.provider.ProviderAttributeNum;
import fr.paris.lutece.plugins.stock.business.category.Category;
import fr.paris.lutece.plugins.stock.business.offer.Offer;
import fr.paris.lutece.plugins.stock.business.product.IProductDAO;
import fr.paris.lutece.plugins.stock.business.product.Product;
import fr.paris.lutece.plugins.stock.service.IDistrictService;
import fr.paris.lutece.plugins.stock.service.IOfferService;
import fr.paris.lutece.plugins.stock.service.IProductService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.url.UrlItem;


/**
 * The indexer service for Solr.
 * 
 */
public class SolrStockIndexer implements SolrIndexer
{
    private static final String FIELD_PROVIDER_ADRESS = "fournisseur_adress";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_PROVIDER = "fournisseur";
    private static final String FIELD_ID = "id";
    private static final String FIELD_FULL = "full";
    private static final String FIELD_TARIF_REDUIT = "tarif_reduit";
    private static final String FIELD_INVITATION = "invitation";
    private static final String FIELD_INVITATION_ENFANT = "invitation_enfant";
    private static final String FIELD_SHOW_DATES = "show_dates";

    // Not used
    // private static final String PARAMETER_SOLR_DOCUMENT_ID = "solr_document_id";
    private static final String PROPERTY_INDEXER_ENABLE = "stock-solr.indexer.enable";
    private static final String PROPERTY_PRODUCT_URL = "stock-solr.indexer.product.url";
    private static final String PROPERTY_NAME = "stock-solr.indexer.name";
    private static final String PROPERTY_DESCRIPTION = "stock-solr.indexer.description";
    private static final String PROPERTY_VERSION = "stock-solr.indexer.version";
    private static final String PROPERTY_SUMMARY_SIZE = "stock-solr.indexer.summary.size";
    private static final String PROPERTY_TYPE = "stock-solr.indexer.type";
    private static final String PARAMETER_STOCK_ID = "product_id";
    private static final List<String> LIST_RESSOURCES_NAME = new ArrayList<String>( );
    private static final String SHORT_NAME = "doc";

    private static final String DOC_INDEXATION_ERROR = "[SolrDocIndexer] An error occured during the indexation of the stock number ";

    @Inject
    @Named( "stock.productService" )
    private IProductService productService;

    @Inject
    @Named( "stock.districtService" )
    private IDistrictService _districtService;
    
    @Inject
    @Named( "stock.offerService" )
    private IOfferService _offerService;
    
    @Inject
    @Named( "stock.productDAO" )
    private IProductDAO _productDAO;

    /**
     * Creates a new SolrPageIndexer
     */
    public SolrStockIndexer( )
    {
        LIST_RESSOURCES_NAME.add( "DOCUMENT_STOCK" );
    }

    public boolean isEnable( )
    {
        return "true".equalsIgnoreCase( AppPropertiesService.getProperty( PROPERTY_INDEXER_ENABLE ) );
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    public List<String> indexDocuments( )
    {
        List<String> lstErrors = new ArrayList<String>( );
        
        List<Product> listProduct = productService.getAllProduct( );
        for ( Product ticketProduct : listProduct )
        {

            try
            {
                // Generates the item to index
                SolrItem item = getItem( ticketProduct );

                if ( item != null )
                {
                    SolrIndexerService.write( item );
                }
            }
            catch ( Exception e )
            {
                lstErrors.add( SolrIndexerService.buildErrorMessage( e ) );
                AppLogService.error( DOC_INDEXATION_ERROR + ticketProduct.getId( ), e );
            }
        }

        return lstErrors;
    }

    /**
     * {@inheritDoc}
     */
    private SolrItem getItem( Product product )
    {
        SolrItem item = null;
        item = new SolrItem( );
        // the item
        item.setUid( getResourceUid( Integer.valueOf( product.getId( ) ).toString( ), "DOCUMENT_STOCK" ) );
        item.setType( AppPropertiesService.getProperty( PROPERTY_TYPE, "PRODUCT" ) );
        item.setSummary( StringUtils.abbreviate( product.getDescription( ),
                AppPropertiesService.getPropertyInt( PROPERTY_SUMMARY_SIZE, 300 ) ) );
        item.setTitle( product.getName( ) );
        item.setSite( SolrIndexerService.getWebAppName( ) );
        item.setRole( "none" );

        UrlItem url = new UrlItem( AppPropertiesService.getProperty( PROPERTY_PRODUCT_URL ) );
        url.addParameter( PARAMETER_STOCK_ID, product.getId( ) );
        item.setUrl( url.getUrl( ) );

        List<String> categories = new ArrayList<String>( );
        Category category = product.getCategory( );
        // Récupère la hierarchie de catégorie
        while ( category != null )
        {
            categories.add( category.getName( ) );
            category = category.getParent( );
        }
        item.setCategorie( categories );

        String keyDate = "date";
        String annuleKey = "annule";
        Timestamp now = new Timestamp(System.currentTimeMillis());

        item.addDynamicFieldNotAnalysed( FIELD_FULL, productService.isFull( product.getId( ) ).toString( ) );
        item.addDynamicFieldNotAnalysed( FIELD_TARIF_REDUIT, productService.isTypeOffer( product.getId( ), 1, keyDate, now, annuleKey ).toString( ) );
        item.addDynamicFieldNotAnalysed( FIELD_INVITATION, productService.isTypeOffer( product.getId( ), 2, keyDate, now, annuleKey ).toString( ) );
        item.addDynamicFieldNotAnalysed( FIELD_INVITATION_ENFANT, productService.isTypeOffer( product.getId( ), 3, keyDate, now, annuleKey )
                .toString( ) );

        item.addDynamicFieldNotAnalysed( FIELD_PROVIDER, product.getProvider( ).getName( ) );
        item.addDynamicFieldNotAnalysed( FIELD_PROVIDER_ADRESS, product.getProvider( ).getAddress( ) );
        item.addDynamicFieldNotAnalysed( FIELD_TITLE, product.getName( ) );

        item.addDynamicFieldNotAnalysed( FIELD_ID, "" + product.getId( ) );

        // Add dynamic attributes
        for ( ProductAttribute attribute : product.getAttributeList( ) )
        {
            item.addDynamicFieldNotAnalysed( attribute.getKey( ), attribute.getValue( ) );
        }

        // Add dynamic attributes
        for ( ProductAttribute attribute : product.getAttributeList( ) )
        {
            item.addDynamicFieldNotAnalysed( attribute.getKey( ), attribute.getValue( ) );
        }

        // Add dynamic attributes
        for ( ProviderAttribute attribute : product.getProvider( ).getAttributeList( ) )
        {
            item.addDynamicFieldNotAnalysed( attribute.getKey( ), attribute.getValue( ) );
        }
        for ( ProviderAttributeNum attribute : product.getProvider( ).getAttributeNumList( ) )
        {
            if ( attribute.getValue( ) != null )
            {
                if ( "district".equals( attribute.getKey( ) ) )
                {
                    int districtId = attribute.getValue( ).intValue( );
                    item.addDynamicFieldNotAnalysed( attribute.getKey( ), _districtService.findLibelleById( districtId ) );
                }
            }
        }
        for ( ProductAttributeDate attribute : product.getAttributeDateList( ) )
        {
            item.addDynamicField( attribute.getKey( ), attribute.getValue( ) );
        }
        for ( ProductAttributeNum attribute : product.getAttributeNumList( ) )
        {
            if ( attribute.getValue( ) != null )
            {
                item.addDynamicField( attribute.getKey( ), attribute.getValue( ).longValue( ) );
            }
        }

        // The content
        item.setContent( product.getName( ) + " - " + product.getDescription( ) + " - "
                + product.getProvider( ).getName( ) );
        
        // Indexing show dates
        List<Offer> offers = _offerService.findByProduct(product.getId());
        
        item.addDynamicFieldListDate(FIELD_SHOW_DATES, offers.stream()
        		.map(Offer::getAttributeDateList)
        		.flatMap(Collection::stream)
        		.filter((OfferAttributeDate oad) -> "date".equals(oad.getKey()))
        		.map(OfferAttributeDate::getValue)
        		.map(Timestamp::toInstant)
        		.map(Date::from)
        		.distinct()
        		.collect(Collectors.toList()));
        return item;
    }

    //GETTERS & SETTERS
    /**
     * Returns the name of the indexer.
     * @return the name of the indexer
     */
    public String getName( )
    {
        return AppPropertiesService.getProperty( PROPERTY_NAME );
    }

    /**
     * Returns the version.
     * @return the version.
     */
    public String getVersion( )
    {
        return AppPropertiesService.getProperty( PROPERTY_VERSION );
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription( )
    {
        return AppPropertiesService.getProperty( PROPERTY_DESCRIPTION );
    }

    /**
     * {@inheritDoc}
     */
    public List<Field> getAdditionalFields( )
    {
        List<Field> lstFields = new ArrayList<Field>( );
        return lstFields;
        // field.setEnableFacet( true );
        // field.setDescription(
        // "Date de début de la période de validité du produit" );
        // field.setIsFacet( true );
        // field.setName( FIELD_DATE_BEGIN + SolrItem.DYNAMIC_DATE_FIELD_SUFFIX
        // );
        // field.setLabel( "Date de début" );
        // lstFields.add( field );
        //
        // field = new Field( );
        // field.setEnableFacet( true );
        // field.setDescription(
        // "Date de fin de la période de validité du produit" );
        // field.setIsFacet( true );
        // field.setName( FIELD_DATE_END + SolrItem.DYNAMIC_DATE_FIELD_SUFFIX );
        // field.setLabel( "Date de fin" );
        // lstFields.add( field );

        // field = new Field( );

        // field = new Field( );
        // field.setEnableFacet( true );
        // field.setDescription( "Prix du produit" );
        // field.setIsFacet( true );
        // field.setName( FIELD_PRICE + SolrItem.DYNAMIC_LONG_FIELD_SUFFIX );
        // field.setLabel( "Prix" );
        // field.setEnableSort( true );
        // lstFields.add( field );
        //
        // field = new Field( );
        // field.setEnableFacet( true );
        // field.setDescription( "Nom du fournisseur" );
        // field.setIsFacet( true );
        // field.setName( FIELD_PROVIDER + SolrItem.DYNAMIC_STRING_FIELD_SUFFIX
        // );
        // field.setLabel( "Fournisseur" );
        // lstFields.add( field );
        // return lstFields;
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    public List<SolrItem> getDocuments( String strIdProduct )
    {
        List<SolrItem> lstItems = null;

        int nIdProduct = Integer.parseInt( strIdProduct );
        Product product = _productDAO.findById(nIdProduct);

        if ( product != null )
        {
            lstItems = new ArrayList<SolrItem>( );
            lstItems.add( getItem( product ) );
        }

        return lstItems;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getResourcesName( )
    {
        return LIST_RESSOURCES_NAME;
    }

    /**
     * {@inheritDoc}
     */
    public String getResourceUid( String strResourceId, String strResourceType )
    {
        StringBuffer sb = new StringBuffer( strResourceId );
        sb.append( SolrConstants.CONSTANT_UNDERSCORE ).append( SHORT_NAME );

        return sb.toString( );
    }

}
