import * as Types from '../../types/__generated__/graphql';

import { gql } from '@apollo/client';
import * as ApolloReactCommon from '@apollo/client/react';
import * as ApolloReactHooks from '@apollo/client/react';
const defaultOptions = {} as const;
export type ConversationQueryVariables = Types.Exact<{
  sessionId: Types.Scalars['String']['input'];
}>;


export type ConversationQuery = { conversation: { __typename: 'Conversation', sessionId: string, messageCount: number, lastMessage: string | null } | null };


export const ConversationDocument = gql`
    query Conversation($sessionId: String!) {
  conversation(sessionId: $sessionId) {
    sessionId
    messageCount
    lastMessage
  }
}
    `;

/**
 * __useConversationQuery__
 *
 * To run a query within a React component, call `useConversationQuery` and pass it any options that fit your needs.
 * When your component renders, `useConversationQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useConversationQuery({
 *   variables: {
 *      sessionId: // value for 'sessionId'
 *   },
 * });
 */
export function useConversationQuery(baseOptions: ApolloReactHooks.QueryHookOptions<ConversationQuery, ConversationQueryVariables> & ({ variables: ConversationQueryVariables; skip?: boolean; } | { skip: boolean; }) ) {
        const options = {...defaultOptions, ...baseOptions}
        return ApolloReactHooks.useQuery<ConversationQuery, ConversationQueryVariables>(ConversationDocument, options);
      }
export function useConversationLazyQuery(baseOptions?: ApolloReactHooks.LazyQueryHookOptions<ConversationQuery, ConversationQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return ApolloReactHooks.useLazyQuery<ConversationQuery, ConversationQueryVariables>(ConversationDocument, options);
        }
export function useConversationSuspenseQuery(baseOptions: ApolloReactHooks.SkipToken | (ApolloReactHooks.SuspenseQueryHookOptions<ConversationQuery, ConversationQueryVariables> & { variables: ConversationQueryVariables })) {
          const options = baseOptions === ApolloReactHooks.skipToken ? baseOptions : {...defaultOptions, ...baseOptions}
          return ApolloReactHooks.useSuspenseQuery<ConversationQuery, ConversationQueryVariables>(ConversationDocument, options);
        }
export type ConversationQueryHookResult = ReturnType<typeof useConversationQuery>;
export type ConversationLazyQueryHookResult = ReturnType<typeof useConversationLazyQuery>;
export type ConversationSuspenseQueryHookResult = ReturnType<typeof useConversationSuspenseQuery>;
export type ConversationQueryResult = ApolloReactCommon.QueryResult<ConversationQuery, ConversationQueryVariables>;