<img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/>

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/composer-adapter)](http://www.rultor.com/p/artipie/composer-adapter)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Build Status](https://img.shields.io/travis/artipie/composer-adapter/master.svg)](https://travis-ci.org/artipie/composer-adapter)
[![Javadoc](http://www.javadoc.io/badge/com.artipie/composer-adapter.svg)](http://www.javadoc.io/doc/com.artipie/composer-adapter)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/composer-adapter/blob/master/LICENSE)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/composer-adapter)](https://hitsofcode.com/view/github/artipie/composer-adapter)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/composer-adapter.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/composer-adapter)
[![PDD status](http://www.0pdd.com/svg?name=artipie/composer-adapter)](http://www.0pdd.com/p?name=artipie/composer-adapter)

This Java library turns your binary [ASTO](https://github.com/artipie/asto) 
storage into a PHP Composer repository.

Similar solutions:

  * [Packagist](https://packagist.org/)
  * [Packagist Private](https://packagist.com/)
  * [Satis](https://github.com/composer/satis)
  * [Artifactory](https://www.jfrog.com/confluence/display/RTF/PHP+Composer+Repositories)

Some valuable references:

  * [Composer Documentation](https://getcomposer.org/doc/)
  * [Packagist Private API](https://packagist.com/docs/api)
  * [Composer GitHub](https://github.com/composer)

## Getting started

This is the dependency you need:

```xml
<dependency>
  <groupId>com.artipie</groupId>
  <artifactId>composer-adapter</artifactId>
  <version>[...]</version>
</dependency>
```

Save PHP Composer package JSON file like `composer.json` (particular name does not matter)
to [ASTO](https://github.com/artipie/asto) storage.

```java
import com.artpie.asto.*;
Storage storage = new FileStorage(Path.of("/path/to/storage"));
storage.save(
    new Key.From("composer.json"), 
    Files.readAllBytes(Path.of("/my/files/composer.json"))
);
```

Then, make an instance of `Repository` class with storage as an argument.
Finally, instruct `Repository` to add the package to repository:

```java
import com.artpie.composer.*;
Repository repo = new Repository(storage);
repo.add(new Key.From("composer.json"));
```

After that package metadata could be accessed by it's name:

```java
Packages packages = repo.packages(new Name("vendor/package"));
```

Read the [Javadoc](http://www.javadoc.io/doc/com.artipie/composer-adapter)
for more technical details.

## How to contribute

Fork repository, make changes, send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install -Pqulice
```

To avoid build errors use Maven 3.2+.

