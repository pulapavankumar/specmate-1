# This is the Specmate UI

## Setup VS Code

Install some extensions to make your life easier:

- Angular (newest version) TypeScript Snippets
- Bootstrap 4 & Font awesome snippets
- NgBootstrap Snippets
- TSLint (from egamma)

## Initialize everything:

- ```npm run init``` (You should run this in the beginning of every development session)

To build a dist: ```npm run build```, or Press ```CTRL+B``` in Visual Studio Code, having this folder opened.

To clean: ```npm run clean```

## compiled

- ```npm run build-dev``` compiles everything and deploys it in a developer friendly version on the server.
- ```npm run build-prod``` compiles everything and deploys it in a production friendly version on the server.

## Utilities

- ```npm run check``` Runs the linter for code errors, prints all licenses used in production mode, and checks whether the dependencies in package.json reference the newest versions of the libraries.
- ```npm run update-dependencies``` Updates all dependencies in ```package.json``` to reflect the newest version. You should use this with care. Additionally, you have to run ```npm run init``` afterwards.

## Structure

All sources are in the folder ```src```. The folder ```webpack``` contains build related files.

### sources

All TypeScript files are contained in the ```app```-folder. Dependent on which kind of functionality is implemented, files go into different folders.

#### config

Here lies only the global configuration of the Specmate-UI.

#### factory

The whole code for creating new objects is here.

#### model

Autogenerated data model classes.

#### modules

The angular components, services, directives and modules.

#### util

Utility functions.

### assets

Go into the ```assets``` folder. Here are also the translation files.

## Build

We use webpack to build the Specmate UI. The build definitions are in the ```webpack```-folder.

Entry points for building the UI are ```main.ts```, ```vendor.ts```, ```polyfills.ts```, ```assets.ts```. 

Typescript files are compiled into specmate, once they are referenced via an import-statement in one of the entry points (or referenced by a file imported into the entry points; transitively).

Assets can be imported either by referencing them in html-files, e.g. images (see index.html; an image is referenced in an ```<img>```-tag; it is automatically included in the build).

## Release

- Set new Release Name (in webpack.common.js)
- Merge into master (Reviewer needed)
- Create a new release in Github with name and tag $(Release Name)
- On the specmate server:
    - Backup database
    - run ./update.sh in GIT/specmate-docker