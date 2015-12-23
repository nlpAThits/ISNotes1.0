
ISNotes 1.0

Developers:


Katja Markert, School of Computing, University of Leeds (markert@comp.leeds.ac.uk)
Yufang Hou, Heidelberg Institute for Theoretical Studies (yufang.hou@h-its.org)
Michael Strube, Heidelberg Institute for Theoretical Studies (michael.strube@h-its.org)



LICENSE:


This corpus contains a part of the OntoNotes corpus annotated for information status (IS) as described in 


Markert, Katja; Hou, Yufang; Strube, Michael (2012).Collective Classification for Fine-grained Information StatusIn: ACL '12, pp.795-804
Hou, Yufang; Markert, Katja; Strube, Michael (2013).Global Inference for Bridging Anaphora Resolution. In: NAACL '13, to appear. 


You are free to use this annotation for your own research without restrictions. You will need a license for the original OntoNotes
data (available from the Linguistic Data Consortium (LDC)). Once you have this data, you can put it in the OntoNotesOriginal subdirectory
in this directory
and use our conversion scripts to convert the original OntoNotes ONF format to the format we use for the IS annotation.



If you use our annotation, please cite the two papers above. 

If you have any questions, please contact Katja Markert at markert@comp.leeds.ac.uk.


##################################



FORMAT: 

The IS annotation is based on the standoff format of the MMAX annotation tool [1]. 
We provide simple scripts to convert the original OntoNotes ONF format to MMAX format (giving you BaseData, sentence and
coreference annotation level) as well as some necessary style files for visualizing  the IS annotation nicely in MMAX.


############################################


DIRECTORY STRUCTURE

1. OntoNotesOriginal: 

As your first action, you will need to put the  orginal OntoNotes ONF files in this subdirectory for the 50 OntoNotes WSJ files that
our IS annotation covers.



The IS annotation covers following 50 OntoNotes WSJ documents:

1004|1123|1200|1327|1428|1017|1137|1215|1353|1435|
1041|1146|1232|1367|1436|1044|1148|1264|1379|1443|
1053|1150|1284|1387|1448|1066|1153|1298|1388|1450|
1094|1160|1310|1397|1455|1100|1163|1313|1404|1465|
1101|1172|1315|1423|1473|1121|1174|1324|1424|2454|




2. ISAnnotationWithTrace: IS level annotation files, corresponding to the BaseData including traces. 

3. ISAnnotationWithoutTrace: IS level annotation files, corresponding to the BaseData without traces.

4. MMAXResourceFile: some necessary style files for visualizing the IS annotation nicely in MMAX. Corefence Mentions are marked
by () and informations status mentions are marked by [].

5. src: JAVA source code which has been  tailored from the Heidelberg Institute for Theoretical Studies internal MMAXTransformer (we thank 
 Simone Paolo Ponzetto and Jie Cai for the development of the original Transformer).

6. doc: two papers based on this corpus.

Markert, Katja; Hou, Yufang; Strube, Michael (2012).Collective Classification for Fine-grained Information StatusIn: ACL '12, pp.795-804
Hou, Yufang; Markert, Katja; Strube, Michael (2013).Global Inference for Bridging Anaphora Resolution. In: NAACL '13, to appear. 

as well as the original IS annotation scheme. Note that all parts of
the annotation are tested for reliability, in particular the
annotation of IS as well as the annotation of antecedents for all anaphora types. The one exception is the annotation of the types of
semantic relations for bridging anaphora which is still experimental and will be further explored in future versions of this corpus.



7. OntoNotes2MMAX.jar and OntoNotes2MMAXWOTrace.jar: executable java jar file to convert OntoNotes ONF file to MMAX IS format.

OntoNotes2MMAX.jar is for IS annotation including traces, OntoNotes2MMAXWOTrace.jar is for IS annotation without traces. 

################################################################



STEPS TO TAKE:


1. Put the original OntoNotes ONF files in the OntoNotesOriginal directory (see above).

2.  Run OntoNotes2MMAX.jar and OntoNotes2MMAXWOTrace.jar in the following manner

> java -jar OntoNotes2MMAX.jar dir_original dir_target dir_resourceFile dir_ISAnnotation
dir_original: dir for OntoNotesOriginal
dir__target: your target dir
dir_resourceFile: dir for resourceFile
dir_ISAnnotation: dir for ISAnnotationWithTrace 

example:
 >java -jar OntoNotes2MMAX.jar  ./OntoNotesOriginal ./IS ./MMAXResourceFile ./ISAnnotationWithTrace

> java -jar OntoNotes2MMAXWOTrace.jar dir_original dir_target dir_resourceFile dir_ISAnnotation
dir_original: dir for OntoNotesOriginal
dir__target: your target dir
dir_resourceFile: dir for resourceFile
dir_ISAnnotation: dir for ISAnnotationWithoutTrace 

example:
 >java -jar OntoNotes2MMAXWOTrace.jar  ./OntoNotesOriginal ./ISClean ./MMAXResourceFile ./ISAnnotationWithoutTrace

This will give you the IS annotation converted into MMAX format.


3. Visualize IS annotation.

Once you finish step 2, 
you can use MMAX to open the .mmax file under target dir. Coreference links, bridging links and comparative anaphora-antecedent
links are marked by green, blue and yellow arrows respectively. You can highlight other things you want by revising the mmax style files.

MMAX can be downloaded from http://mmax2.sourceforge.net/. If you use MMAX, please cite the paper


Christoph Müller, Michael Strube (2006): Multi-Level Annotation of Linguistic Data with MMAX2. In: Sabine Braun, Kurt Kohn, Joybrato Mukherjee (Eds.): Corpus Technology and Language Pedagogy. New Resources, New Tools, New Methods. Frankfurt: Peter Lang, pp. 197-214. (English Corpus Linguistics, Vol.3 )


You can also use the standoff data without MMAX of course.
