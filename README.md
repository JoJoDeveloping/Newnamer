#What is this

This is supposed to be a new intermediary name generator for srg names

#How does this work

We parse the TSRG, then we load the supplied classpath into a ClassLoader.
We then visit each class, and assign a new name for it if it is not already named
After that we visit each method/field and assign names, taking old names, `main`, constructors and synthetic bridges into account.