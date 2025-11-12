import { gql } from '@apollo/client';
import { CATALOG_ITEM_FIELDS } from '../../fragments/catalog';

// Save a catalog item
export const SAVE_CATALOG_ITEM = gql`
  ${CATALOG_ITEM_FIELDS}
  mutation SaveCatalogItem($input: CatalogItemInput!) {
    saveCatalogItem(input: $input) {
      ...CatalogItemFields
    }
  }
`;

