package fr.paris.lutece.plugins.stock.modules.solr.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;

import fr.paris.lutece.plugins.search.solr.business.indexeraction.SolrIndexerAction;
import fr.paris.lutece.plugins.search.solr.business.indexeraction.SolrIndexerActionHome;
import fr.paris.lutece.plugins.search.solr.service.SolrPlugin;
import fr.paris.lutece.plugins.stock.business.attribute.category.CategoryAttribute;
import fr.paris.lutece.plugins.stock.business.attribute.category.CategoryAttributeDate;
import fr.paris.lutece.plugins.stock.business.attribute.category.CategoryAttributeNum;
import fr.paris.lutece.plugins.stock.business.attribute.offer.OfferAttribute;
import fr.paris.lutece.plugins.stock.business.attribute.offer.OfferAttributeDate;
import fr.paris.lutece.plugins.stock.business.attribute.offer.OfferAttributeNum;
import fr.paris.lutece.plugins.stock.business.attribute.product.ProductAttribute;
import fr.paris.lutece.plugins.stock.business.attribute.product.ProductAttributeDate;
import fr.paris.lutece.plugins.stock.business.attribute.product.ProductAttributeNum;
import fr.paris.lutece.plugins.stock.business.attribute.provider.ProviderAttribute;
import fr.paris.lutece.plugins.stock.business.attribute.provider.ProviderAttributeDate;
import fr.paris.lutece.plugins.stock.business.attribute.provider.ProviderAttributeNum;
import fr.paris.lutece.plugins.stock.business.category.Category;
import fr.paris.lutece.plugins.stock.business.offer.Offer;
import fr.paris.lutece.plugins.stock.business.product.Product;
import fr.paris.lutece.plugins.stock.business.product.ProductFilter;
import fr.paris.lutece.plugins.stock.business.provider.Provider;
import fr.paris.lutece.plugins.stock.service.IProductService;
import fr.paris.lutece.portal.business.indexeraction.IndexerAction;
import fr.paris.lutece.portal.service.plugin.PluginService;

public class StockSolrProductListener
{

    private static final String TYPE_RESOURCE = "DOCUMENT_STOCK";

    @Inject
    @Named( "stock.productService" )
    IProductService _productService;
    
    @PostUpdate
    public void postUpdate( Object entity )
    {
        processProduct( entity, IndexerAction.TASK_MODIFY );
        processCategory( entity, IndexerAction.TASK_MODIFY );
        processProvider( entity, IndexerAction.TASK_MODIFY );
        processOffer( entity, IndexerAction.TASK_MODIFY );
    }

    @PostPersist
    public void postPersist( Object entity )
    {
        processProduct( entity, IndexerAction.TASK_CREATE );
        processCategory( entity, IndexerAction.TASK_MODIFY );
        processProvider( entity, IndexerAction.TASK_MODIFY );
        processOffer( entity, IndexerAction.TASK_MODIFY );
    }

    @PostRemove
    public void postRemove( Object entity )
    {
        processProduct( entity, IndexerAction.TASK_DELETE );
        processCategory( entity, IndexerAction.TASK_MODIFY );
        processProvider( entity, IndexerAction.TASK_MODIFY );
        processOffer( entity, IndexerAction.TASK_MODIFY );
    }

    private void processProduct( Object entity, int nTask )
    {
        Product product = getProductEntity( entity );
        if ( product != null && !StockSolrService.isSolrIndexerActionExists( product.getId( ), nTask, TYPE_RESOURCE ) )
        {
            SolrIndexerAction indexerAction = new SolrIndexerAction( );
            indexerAction.setIdDocument( product.getId( ).toString( ) );
            indexerAction.setIdTask( nTask );
            indexerAction.setTypeResource( TYPE_RESOURCE );
            SolrIndexerActionHome.create( indexerAction, PluginService.getPlugin( SolrPlugin.PLUGIN_NAME ) );
        }
    }

    private void processProvider( Object entity, int nTask )
    {
        Provider provider = getProviderEntity( entity );
        if ( provider != null )
        {
            List<Category> listCategory = provider.getProducts( );
            List<Product> listProduct = new ArrayList<>( );
            if ( listCategory != null )
            {
                listCategory.forEach(category -> listProduct.addAll(getCategoryProducts(category)));
            }
            processProductsFromCategory( nTask, listProduct );
        }
    }

    private void processCategory( Object entity, int nTask )
    {
        Category category = getCategoryEntity( entity );
        if ( category != null )
        {
            List<Product> listProduct = new ArrayList<>( );
            listProduct.addAll( getCategoryProducts( category ) );
            processProductsFromCategory( nTask, listProduct );
        }
    }

    private void processOffer( Object entity, int nTask )
    {
        Offer offer = getOfferEntity( entity );
        if ( offer != null )
        {
            processProduct( offer.getProduct( ), nTask );
        }
    }

    private void processProductsFromCategory( int nTask, List<Product> listProduct )
    {
        List<Product> listProductWithoutDuplicates = listProduct.stream( ).distinct( ).collect( Collectors.toList( ) );
        if ( !listProductWithoutDuplicates.isEmpty( ) )
        {
            List<SolrIndexerAction> actions = SolrIndexerActionHome.getList( PluginService.getPlugin( SolrPlugin.PLUGIN_NAME ) );
            for ( Product product : listProductWithoutDuplicates )
            {
                if ( !StockSolrService.isActionExists( product.getId( ), nTask, TYPE_RESOURCE, actions ) )
                {
                    SolrIndexerAction indexerAction = new SolrIndexerAction( );
                    indexerAction.setIdDocument( product.getId( ).toString( ) );
                    indexerAction.setIdTask( nTask );
                    indexerAction.setTypeResource( TYPE_RESOURCE );
                    SolrIndexerActionHome.create( indexerAction, PluginService.getPlugin( SolrPlugin.PLUGIN_NAME ) );
                }
            }
        }
    }

    private List<Product> getCategoryProducts( Category category )
    {
        List<Product> listProducts = new ArrayList<>( );
        if ( category != null )
        {
            if ( category.getProductSet( ) == null || category.getProductSet( ).isEmpty( ) )
            {
                ProductFilter productFilter = new ProductFilter( );
                productFilter.setIdCategory( category.getId( ) );
                listProducts.addAll( _productService.findByFilter( productFilter ) );
            }

            if ( listProducts.isEmpty( ) && category.getProductSet( ) != null && !category.getProductSet( ).isEmpty( ) )
            {
                listProducts.addAll( category.getProductSet( ) );
            }

            if ( category.getChildrenList( ) != null && !category.getChildrenList( ).isEmpty( ) )
            {
                Set<Category> categoryChildrens = category.getChildrenList( );
                categoryChildrens.forEach( categoryChildren -> listProducts.addAll( getCategoryProducts( categoryChildren ) ) );
            }
        }
        return listProducts;

    }

    private Offer getOfferEntity( Object entity )
    {
        Offer offer = null;
        if ( entity instanceof Offer )
        {
            offer = ( Offer ) entity;
        }
        if ( entity instanceof OfferAttribute )
        {
            OfferAttribute offerAttribute = ( OfferAttribute ) entity;
            offer = offerAttribute.getOwner( );
        }
        if ( entity instanceof OfferAttributeDate )
        {
            OfferAttributeDate offerAttributeDate = ( OfferAttributeDate ) entity;
            offer = offerAttributeDate.getOwner( );
        }
        if ( entity instanceof OfferAttributeNum )
        {
            OfferAttributeNum offerAttributeNum = ( OfferAttributeNum ) entity;
            offer = offerAttributeNum.getOwner( );
        }
        return offer;
    }

    private Category getCategoryEntity( Object entity )
    {
        Category category = null;
        if ( entity instanceof Category )
        {
            category = ( Category ) entity;
        }
        if ( entity instanceof CategoryAttribute )
        {
            CategoryAttribute categoryAttribute = ( CategoryAttribute ) entity;
            category = categoryAttribute.getOwner( );
        }
        if ( entity instanceof CategoryAttributeDate )
        {
            CategoryAttributeDate categoryAttributeDate = ( CategoryAttributeDate ) entity;
            category = categoryAttributeDate.getOwner( );
        }
        if ( entity instanceof CategoryAttributeNum )
        {
            CategoryAttributeNum categoryAttributeNum = ( CategoryAttributeNum ) entity;
            category = categoryAttributeNum.getOwner( );
        }
        return category;
    }

    private Product getProductEntity( Object entity )
    {
        Product product = null;
        if ( entity instanceof Product )
        {
            product = ( Product ) entity;
        }
        if ( entity instanceof ProductAttribute )
        {
            ProductAttribute productAttribute = ( ProductAttribute ) entity;
            product = productAttribute.getOwner( );
        }
        if ( entity instanceof ProductAttributeDate )
        {
            ProductAttributeDate productAttributeDate = ( ProductAttributeDate ) entity;
            product = productAttributeDate.getOwner( );
        }
        if ( entity instanceof ProductAttributeNum )
        {
            ProductAttributeNum productAttributeNum = ( ProductAttributeNum ) entity;
            product = productAttributeNum.getOwner( );
        }
        return product;
    }

    private Provider getProviderEntity( Object entity )
    {
        Provider provider = null;
        if ( entity instanceof Provider )
        {
            provider = ( Provider ) entity;
        }
        if ( entity instanceof ProviderAttribute )
        {
            ProviderAttribute providerAttribute = ( ProviderAttribute ) entity;
            provider = providerAttribute.getOwner( );
        }
        if ( entity instanceof ProviderAttributeDate )
        {
            ProviderAttributeDate providerAttributeDate = ( ProviderAttributeDate ) entity;
            provider = providerAttributeDate.getOwner( );
        }
        if ( entity instanceof ProviderAttributeNum )
        {
            ProviderAttributeNum providerAttributeNum = ( ProviderAttributeNum ) entity;
            provider = providerAttributeNum.getOwner( );
        }

        return provider;
    }
}
