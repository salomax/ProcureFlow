import * as Types from '../../types/__generated__/graphql';

import { gql } from '@apollo/client';
import { CatalogItemFieldsFragmentDoc } from '../../fragments/catalog.generated';
import * as ApolloReactCommon from '@apollo/client/react';
import * as ApolloReactHooks from '@apollo/client/react';
const defaultOptions = {} as const;
export type SearchCatalogItemsQueryVariables = Types.Exact<{
  query: Types.Scalars['String']['input'];
}>;


export type SearchCatalogItemsQuery = { searchCatalogItems: Array<{ __typename: 'CatalogItem', id: string, name: string, category: Types.CatalogItemCategory, priceCents: number, status: Types.CatalogItemStatus, description: string | null, createdAt: string, updatedAt: string }> };

export type GetCatalogItemQueryVariables = Types.Exact<{
  id: Types.Scalars['ID']['input'];
}>;


export type GetCatalogItemQuery = { catalogItem: { __typename: 'CatalogItem', id: string, name: string, category: Types.CatalogItemCategory, priceCents: number, status: Types.CatalogItemStatus, description: string | null, createdAt: string, updatedAt: string } | null };


export const SearchCatalogItemsDocument = gql`
    query SearchCatalogItems($query: String!) {
  searchCatalogItems(query: $query) {
    ...CatalogItemFields
  }
}
    ${CatalogItemFieldsFragmentDoc}`;

/**
 * __useSearchCatalogItemsQuery__
 *
 * To run a query within a React component, call `useSearchCatalogItemsQuery` and pass it any options that fit your needs.
 * When your component renders, `useSearchCatalogItemsQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useSearchCatalogItemsQuery({
 *   variables: {
 *      query: // value for 'query'
 *   },
 * });
 */
export function useSearchCatalogItemsQuery(baseOptions: ApolloReactHooks.QueryHookOptions<SearchCatalogItemsQuery, SearchCatalogItemsQueryVariables> & ({ variables: SearchCatalogItemsQueryVariables; skip?: boolean; } | { skip: boolean; }) ) {
        const options = {...defaultOptions, ...baseOptions}
        return ApolloReactHooks.useQuery<SearchCatalogItemsQuery, SearchCatalogItemsQueryVariables>(SearchCatalogItemsDocument, options);
      }
export function useSearchCatalogItemsLazyQuery(baseOptions?: ApolloReactHooks.LazyQueryHookOptions<SearchCatalogItemsQuery, SearchCatalogItemsQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return ApolloReactHooks.useLazyQuery<SearchCatalogItemsQuery, SearchCatalogItemsQueryVariables>(SearchCatalogItemsDocument, options);
        }
export function useSearchCatalogItemsSuspenseQuery(baseOptions: ApolloReactHooks.SkipToken | (ApolloReactHooks.SuspenseQueryHookOptions<SearchCatalogItemsQuery, SearchCatalogItemsQueryVariables> & { variables: SearchCatalogItemsQueryVariables })) {
          const options = baseOptions === ApolloReactHooks.skipToken ? baseOptions : {...defaultOptions, ...baseOptions}
          return ApolloReactHooks.useSuspenseQuery<SearchCatalogItemsQuery, SearchCatalogItemsQueryVariables>(SearchCatalogItemsDocument, options);
        }
export type SearchCatalogItemsQueryHookResult = ReturnType<typeof useSearchCatalogItemsQuery>;
export type SearchCatalogItemsLazyQueryHookResult = ReturnType<typeof useSearchCatalogItemsLazyQuery>;
export type SearchCatalogItemsSuspenseQueryHookResult = ReturnType<typeof useSearchCatalogItemsSuspenseQuery>;
export type SearchCatalogItemsQueryResult = ApolloReactCommon.QueryResult<SearchCatalogItemsQuery, SearchCatalogItemsQueryVariables>;
export const GetCatalogItemDocument = gql`
    query GetCatalogItem($id: ID!) {
  catalogItem(id: $id) {
    ...CatalogItemFields
  }
}
    ${CatalogItemFieldsFragmentDoc}`;

/**
 * __useGetCatalogItemQuery__
 *
 * To run a query within a React component, call `useGetCatalogItemQuery` and pass it any options that fit your needs.
 * When your component renders, `useGetCatalogItemQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useGetCatalogItemQuery({
 *   variables: {
 *      id: // value for 'id'
 *   },
 * });
 */
export function useGetCatalogItemQuery(baseOptions: ApolloReactHooks.QueryHookOptions<GetCatalogItemQuery, GetCatalogItemQueryVariables> & ({ variables: GetCatalogItemQueryVariables; skip?: boolean; } | { skip: boolean; }) ) {
        const options = {...defaultOptions, ...baseOptions}
        return ApolloReactHooks.useQuery<GetCatalogItemQuery, GetCatalogItemQueryVariables>(GetCatalogItemDocument, options);
      }
export function useGetCatalogItemLazyQuery(baseOptions?: ApolloReactHooks.LazyQueryHookOptions<GetCatalogItemQuery, GetCatalogItemQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return ApolloReactHooks.useLazyQuery<GetCatalogItemQuery, GetCatalogItemQueryVariables>(GetCatalogItemDocument, options);
        }
export function useGetCatalogItemSuspenseQuery(baseOptions: ApolloReactHooks.SkipToken | (ApolloReactHooks.SuspenseQueryHookOptions<GetCatalogItemQuery, GetCatalogItemQueryVariables> & { variables: GetCatalogItemQueryVariables })) {
          const options = baseOptions === ApolloReactHooks.skipToken ? baseOptions : {...defaultOptions, ...baseOptions}
          return ApolloReactHooks.useSuspenseQuery<GetCatalogItemQuery, GetCatalogItemQueryVariables>(GetCatalogItemDocument, options);
        }
export type GetCatalogItemQueryHookResult = ReturnType<typeof useGetCatalogItemQuery>;
export type GetCatalogItemLazyQueryHookResult = ReturnType<typeof useGetCatalogItemLazyQuery>;
export type GetCatalogItemSuspenseQueryHookResult = ReturnType<typeof useGetCatalogItemSuspenseQuery>;
export type GetCatalogItemQueryResult = ApolloReactCommon.QueryResult<GetCatalogItemQuery, GetCatalogItemQueryVariables>;