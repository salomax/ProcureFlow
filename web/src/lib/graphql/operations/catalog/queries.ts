import { gql } from '@apollo/client';
import { CATALOG_ITEM_FIELDS } from '../../fragments/catalog';

// Search catalog items by query string
export const SEARCH_CATALOG_ITEMS = gql`
  ${CATALOG_ITEM_FIELDS}
  query SearchCatalogItems($query: String!) {
    searchCatalogItems(query: $query) {
      ...CatalogItemFields
    }
  }
`;

// Get single catalog item by ID
export const GET_CATALOG_ITEM = gql`
  ${CATALOG_ITEM_FIELDS}
  query GetCatalogItem($id: ID!) {
    catalogItem(id: $id) {
      ...CatalogItemFields
    }
  }
`;

