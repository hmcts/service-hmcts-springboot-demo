# Mapping

Often we map between java objects and json strings so we can serialise a java object before we pass it around
or store it.
We have various tools for doing this with the most common being Jackson and Gson.

We use a jackson fasterxml to do this
It makes sense to use a common mapper service to do this to ensure it behaves consistently.
In particular we need to configure the mapper to handle dates in our preferred format which is UTC
( The jackson default for date is as a long value )

