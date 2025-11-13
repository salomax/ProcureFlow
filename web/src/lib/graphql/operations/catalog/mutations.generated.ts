import * as Types from '../../types/__generated__/graphql';

import { gql } from '@apollo/client';
import { CatalogItemFieldsFragmentDoc } from '../../fragments/catalog.generated';
import * as ApolloReactCommon from '@apollo/client/react';
import * as ApolloReactHooks from '@apollo/client/react';
const defaultOptions = {} as const;
export type SaveCatalogItemMutationVariables = Types.Exact<{
  input: Types.CatalogItemInput;
}>;


export type SaveCatalogItemMutation = { saveCatalogItem: { __typename: 'CatalogItem', id: string, name: string, category: Types.CatalogItemCategory, priceCents: number, status: Types.CatalogItemStatus, description: string | null, createdAt: string, updatedAt: string } };


export const SaveCatalogItemDocument = gql`
    mutation SaveCatalogItem($input: CatalogItemInput!) {
  saveCatalogItem(input: $input) {
    ...CatalogItemFields
  }
}
    ${CatalogItemFieldsFragmentDoc}`;

/**
 * __useSaveCatalogItemMutation__
 *
 * To run a mutation, you first call `useSaveCatalogItemMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useSaveCatalogItemMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [saveCatalogItemMutation, { data, loading, error }] = useSaveCatalogItemMutation({
 *   variables: {
 *      input: // value for 'input'
 *   },
 * });
 */
export function useSaveCatalogItemMutation(baseOptions?: ApolloReactHooks.MutationHookOptions<SaveCatalogItemMutation, SaveCatalogItemMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return ApolloReactHooks.useMutation<SaveCatalogItemMutation, SaveCatalogItemMutationVariables>(SaveCatalogItemDocument, options);
      }
export type SaveCatalogItemMutationHookResult = ReturnType<typeof useSaveCatalogItemMutation>;
export type SaveCatalogItemMutationResult = ApolloReactCommon.MutationResult<SaveCatalogItemMutation>;
