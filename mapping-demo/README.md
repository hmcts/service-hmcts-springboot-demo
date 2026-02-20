# Mapping

In our rest endpoints we do a lot of mapping between objects in different layers
We try to avoid exposing objects from the layer below
i.e. We do not expose repository entity fields out to consumers
Instead we map to a more neutral object

Sometimes the mapping may feel pointless as we may have the same fields on both sides
In this case we can and should use MapStruct to get the mappings for free with the minimum of code

Often we map between java objects and json strings
We have various tools for doing this with the most common being Jackson and Gson.

Sometimes we need to configure the mapper to handle dates in our preferred format which is UTC

... TBC would like a global mapper to be available across our app to avoid duplication and differing styles

