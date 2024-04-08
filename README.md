# jerbil
A *very* simple fast file-based CMS: markdown format text files + html templates.

Status: alpha   
We use this tool internally and it is 100% ready. We are working on nice packaging for others to use.

## Basic use

1. Install: either download the jerbil-all jar, *or if you use `npm`*, install by: `npm -g i jerbil-cms`
1. Clone the [example jerbil site](https://github.com/winterstein/jerbil-website) to get started quickly.
2. Write content in the `pages` directory. You can mix markdown and html.
3. Run Jerbil: `java -jar jerbil-all.jar` in your project's directory.    
Or, if you used npm to install jerbil-cms, just type `jerbil` in the project directory.

## Variables

You can define variables at the start of a page file, using the simple format:

  key: value

Then use them within templates or page contents via `$key`.

A couple of variables are defined by default (you can override them). These are:

 - `$title` default value: the filename, without the .html / .txt type 
 - `$date` default value: the date when run

## Templates and Imports: `<section>`

You can pull content from other files into a page and use templates. E.g.

* `<section src='myfooter' />` Load myfooter.html or myfooter.md and insert it here.
* `<section src='myarticletemplate.html'>Blah blah</section>` Use myarticletemplate.html as a template with the contents "Blah blah".	


## Developer Installation

Depends on: the libraries in the [open-code](https://github.com/sodash/open-code) repo.

## Credits

Built using Java by the [Good-Loop](http://good-loop.com/?utm_source=winterstein&utm_medium=code&utm_campaign=jerbil) team.
