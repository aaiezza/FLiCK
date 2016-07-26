# FLiCK
###&nbsp;&nbsp; [File] <i>F</i>ormat <i>L</i>everag<i>i</i>ng <i>C</i>ompression framewor<i>k</i>

&nbsp;&nbsp;A Bioinformatics thesis project by **Alessandro Aiezza II**<br/>
&nbsp;&nbsp;&nbsp;&nbsp;Defended on July 20, 2016 @ the [Rochester Institute of Technology](https://www.rit.edu/cos/bioinformatics/about.html)

&nbsp;**Committee**<br/>
&nbsp;&nbsp;&nbsp;&nbsp;[Dr. Gary Skuse](https://www.rit.edu/science/people/gary-skuse), [Dr. Greg Babbitt](https://www.rit.edu/science/people/gregory-babbitt), [Dr. Larry Buckley](https://www.rit.edu/science/people/larry-buckley)

A **Java** framework that makes it easier to develop file compressors/decompressors by leveraging _ab inito_ knowledge about a specific file format. `FLiCK` runs independently as a file compressor and currently will ZIP any files it is given.

A developer can create a module in `FLiCK` for any file format. A **module** associates a file's format with one or many file extension names. (For example, the FASTA module will work on files with extenstions `.fa`, `.fasta`, and `.fna`.) When the classes or jar of a  `FLiCK` module is found on the `CLASSPATH` at runtime, `FLiCK` will check for all associated file names and use a module's compression algorithm as oppose to the default ZIP algorithm.

_FLiCK comes preloaded with FASTA and FASTQ file format compression modules_

------------------------------------------------------------

## Usage - users
1. Download from release page [FLiCK Releases](https://github.com/aaiezza/FLiCK/releases)
2. Untarball/unzip contents into a directory on your `PATH`
  - flick.jar
  - flick _(executable)_
  - unflick _(executable)_
3. You should be ready to go!
  ![FLiCK User tutorial][flick-tutorial]
  ![unFLiCK User tutorial][unflick-tutorial]

------------------------------------------------------------

## Usage - Developers (Module Creation)
1. Download flick.jar from the [releases page](https://github.com/aaiezza/FLiCK/releases) and add to `CLASSPATH`

  ```bash
  $ export CLASSPATH=path/to/other/jars:flick.jar
  ```
2. **Five classes** need to be implemented to create a module:

  [`FileDeflator`](https://github.com/aaiezza/FLiCK/blob/master/src/edu/rit/flick/FileDeflator.java) | [`FileInflator`](https://github.com/aaiezza/FLiCK/blob/master/src/edu/rit/flick/FileInflator.java) | [`DeflationOptionSet`](https://github.com/aaiezza/FLiCK/blob/master/src/edu/rit/flick/config/DeflationOptionSet.java) | [`InflationOptionSet`](https://github.com/aaiezza/FLiCK/blob/master/src/edu/rit/flick/config/InflationOptionSet.java) | [`FileArchiver`](https://github.com/aaiezza/FLiCK/blob/master/src/edu/rit/flick/FileArchiver.java)
  --- | --- | --- | --- | ---
  Implementation of the file format `compression` algorithm | Implementation of the file format `decompression` algorithm | _Options/flags_ available for altering the behavior and of the algorithm responsible for file `compression` | _Options/flags_ available for altering the behavior and of the algorithm responsible for file `decompression` | **(1)** Holds aspects that are important to both the `deflator` and `inflator`. **(2)** Connects other 4 classes together. **(3)** Declares file extensions the module is appropriate for.
  1. The `FileArchiver` class must be annotated with the [`RegisterFileDeflatorInflator`](https://github.com/aaiezza/FLiCK/blob/master/src/edu/rit/flick/RegisterFileDeflatorInflator.java "The Module Declaration Annotation") class to identify the class names of the other 4 component classes as well as to list what file extensions the module should be used for.<br/>
    &nbsp;&nbsp;(It is recommended to _jar_ your implementing classes for ease of use and portability of your module.)

3. Place your classes (or jar) on the `CLASSPATH` so that they are visible to `FLiCK` at runtime.

------------------------------------------------------------

## FASTA and FASTQ File Format Modules come preloaded in FLiCK

The entirety of both these modules exists in the [`edu.rit.flick.genetics`](https://github.com/aaiezza/FLiCK/tree/master/src/edu/rit/flick/genetics) package. The `FLiCK` [platform] is fully functional and executable without this package, as the package serves as an outside module.

#### FASTA & FASTQ file format specification
<img src="https://lh3.googleusercontent.com/-tTZuF_cABQs/V5dzjRtft1I/AAAAAAAAErw/Sjek0Y8KeaEAO78sQohbNqAqVHa1iG4ogCCo/s1025/FASTformat.jpg" alt="FASTA & FASTQ file format specification" width="500">

#### Architecture of FLiCK
![FLiCK UML Diagram][flick-uml]

#### Example Module Registration for the [FLiCK FASTA compression module][fasta-module-registration]
```java
@RegisterFileDeflatorInflator (
    deflatedExtension = FastaFileArchiver.DEFAULT_DEFLATED_FASTA_EXTENSION,
    inflatedExtensions =
{ "fna", "fa", "fasta" },
    fileDeflator = FastaFileDeflator.class,
    fileInflator = FastaFileInflator.class,
    fileDeflatorOptionSet = FastaDeflationOptionSet.class,
    fileInflatorOptionSet = FastaInflationOptionSet.class )
public interface FastaFileArchiver extends FastFileArchiver
{ ...
    public static final String DEFAULT_DEFLATED_FASTA_EXTENSION       = ".flickfa";
... }
```

## More details behind sample FASTA/FASTQ module implementations
The modules use a [2-bit compression algorithm](https://github.com/aaiezza/FLiCK/blob/kmer-compression/src/edu/rit/flick/genetics/TwoBitNucleotideConverter.java#L16-L17) for the nucleotides:

  Nucleotide | Mapped bits
  :---: | :---:
  `A` | `00`
  `C` | `01`
  `G` | `10`
  `T` | `11`

**Example:** `ACTGATTACA` → `00011110001111000100` → 123844

## FLiCK FASTQ 2-bit compression module performance analysis

Program | Average Compression Ratio | Average Compression Runtime | Average Decompression Runtime
:---: | :---: | :---: | :---:
Path Encoding | `90.9%` | - | -
LW-FQZip | `80.5%` | `44:39` | `02:52`
**`FLiCK`**<br/>_(2-bit module)_ | `77.3%` | `31:55` | `20:46`
gzip | `75.6%` | `19:03` | `10:24`
bzip2 | `78.3%` | `32:18` | `16:33`
Quip | `77.3%` | `11:52` | `01:57`
**_LEON_** | **`91.5%`** | **`32:10`** | **`07:52`**

![FLiCK 2-bit module performance][flick-2bit-performance]

[flick-tutorial]: https://lh3.googleusercontent.com/DYWfbBpoBdo4vWGTj4qHhph4YHesEiuP5rcI4-fCTJn5WVI8zMBmq83E4FRHOjg8JdEsY0Pq18I-GEDQM08mVEHykrl2w9G6Xts88aPXP7w6El5zRzxaxnkg55eWoS-QfaaMKL5gFMvFvuCg7r_6i0SozDAel9y5Sup-tgbG-kS9RLCWLVmoV3OwN6yye8ugI8ELd-FnPTKRNoKBDqFZk8OkY7h-nhlzvSM1jLohgO5S-DWRNSNEuPFjdPlSoXLtsGj-zkroLuLyVfDxGKO7vl3lFHzWI0-vU0NX2qqb4PsOipH9HZsEhZPZBWn3hIZoRw1otYhIkMneXSxZcVi7ZNz_0kX-BNoUjq9xM2HrDmtgQRiwBObGTHe-GvEsk3zZP9FwnhdDgzvUOTQsCRrfUFaCgKqmAZprWxkCVbrBlMRctJ7m-XFIH32U-JD3KMUV-smILac78KpcvMEseSKQgh7TZTr6CXwvaEtgWsX7jF8elr_-mMmgDDSBHAUmyjJrQFa6Qxz0X4bZPrClLsTs6tzSTP1MFvpPtQQnjUNt-jPiuH57WUL2e6MYJmGrL5X2MGe37Md-DXsp28ktCVqfoiIhpbxDKnU=w720-h464-no
[unflick-tutorial]: https://lh3.googleusercontent.com/IRxD682d0eC64LZvlQ7lZE_Q_Z1J6zRKIPBF0ikBs6q5hN1_kVLuQf6E2OBjmyZFukruSfMzOeRE1EZodhRksRgrwshoKyKTqSuCfJb1-YNw5gMOpuICB5MasFLhcon2je-IanKbm33n-9O_wiRWbgwZDaBdFRi2d41upJ70_GVsh0ypez3CiY4UssNAhl6yUQddxGmd3HdtH3K_NlZE0qW71NReuujxONUbWuIReJrURl3ThDIaFc9HpEoi0JRhNv7FbsEni8u14JYw4D3OyVXQgukTt_iJqv8iEkDJyGAOqURkc39Si8b4G-d1LT9Td-NlGAuBV3hE7XkzCBTP6R8VkiA3EJacB7TNNyy7rm5rfV-9OClnja_4caa6Eruq4t_hdVAyydPRWGDyKdKRUzhwjTuYdLpZ9uXzdKNi_aqEiYlain6gPTIfdRE4U6w2tnXN5AGeb10w3Bhvc-RTRimSOq-OPcrS-mZn9KYLa1pgwz9x3vakYr7qgnMicqcacRLz8ZurIGSMKxaekMUB7_6Z2ZXLGusEZSkICelzBoYaWBgMFy8OtfjeJsWA8W5cHiFcXLNzbXtQEfsMM3S9P2WCjSAW3nU=w720-h464-no
[flick-uml]: https://lh3.googleusercontent.com/-D2zFe4hfyv4/V5dzjacZf1I/AAAAAAAAErs/bVHvPb72ED8o9RJsBtFgclx8u8b--j13ACCo/s1079/FLiCK_architecture.png

[fasta-module-registration]: https://github.com/aaiezza/FLiCK/blob/master/src/edu/rit/flick/genetics/FastaFileArchiver.java#L19-L27
[flick-2bit-performance]: https://lh3.googleusercontent.com/-YM4QGWTgTdI/V5dzjfnZa8I/AAAAAAAAEro/7HSpSKKzLuAh5vCosNSJ4oOfLdiNpnQqQCCo/s872/flick_2bit_compare.jpg
