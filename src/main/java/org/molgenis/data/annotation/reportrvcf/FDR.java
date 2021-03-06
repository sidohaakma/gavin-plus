package org.molgenis.data.annotation.reportrvcf;

import org.molgenis.calibratecadd.support.GavinUtils;
import org.molgenis.data.annotation.makervcf.positionalstream.MatchVariantsToGenotypeAndInheritance;
import org.molgenis.data.annotation.makervcf.structs.RVCF;
import org.molgenis.data.annotation.makervcf.structs.AnnotatedVcfRecord;
import org.molgenis.vcf.VcfReader;
import org.molgenis.vcf.VcfRecord;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by joeri on 6/29/16.
 *
 * False Discovery Rate
 *
 *            //FDR: report false hits per gene, right before the stream is swapped from 'gene based' to 'position based'
             //FOR: report missed hits per gene, same as above with pathogenic gold standard set
             //Iterator<GavinRecord> rv8 = new FDR(rv7, new File("/Users/joeri/Desktop/1000G_diag_FDR/exomePlus/FDR.tsv"), verbose).go();
             //Iterator<GavinRecord> rv8 = new FOR(rv7, inputVcfFile).go();

 */
public class FDR {

    private VcfReader vcf;
    private PrintWriter pw;
    int nrOfSamples;

    public static final String HEADER = "Gene" + "\t" + "AffectedAbs" + "\t" + "CarrierAbs" + "\t" + "AffectedFrac" + "\t" + "CarrierFrac";

    public static void main(String[] args) throws Exception {
        String mtRvcfFileName = args[0];
        String fdrFileName = args[1];
        FDR fdr = new FDR(new File(mtRvcfFileName),
                new File(fdrFileName), 2504);
        fdr.go();
    }


    public FDR(File rvcfInput, File outputFDR, int nrOfSamples) throws Exception {
        this.vcf = GavinUtils.getVcfReader(rvcfInput);
        this.pw = new PrintWriter(outputFDR);
        this.nrOfSamples = nrOfSamples;
    }

    public void go() throws Exception {

        Map<String, Integer> geneToAffected = new HashMap<>();
        Map<String, Integer> geneToCarrier = new HashMap<>();

        //make sure we only count every sample once per gene
        Set<String> sampleGeneCombo = new HashSet<>();

        for (VcfRecord aVcf : vcf)
        {

            AnnotatedVcfRecord record = new AnnotatedVcfRecord(aVcf);

            //TODO JvdV: check implications of this being a loop now instead of 1 rvcf
            for (RVCF rvcf : record.getRvcf())
            {

                String gene = rvcf.getGene();

                if (!geneToAffected.containsKey(gene))
                {
                    geneToAffected.put(gene, 0);
                    geneToCarrier.put(gene, 0);
                }

                for (String sample : rvcf.getSampleStatus().keySet())
                {
                    if (MatchVariantsToGenotypeAndInheritance.Status.isPresumedAffected(
                            rvcf.getSampleStatus().get(sample)))
                    {
                        if (!sampleGeneCombo.contains(gene + "_" + sample))
                        {
                            int count = geneToAffected.get(gene);
                            geneToAffected.put(gene, count + 1);
                            sampleGeneCombo.add(gene + "_" + sample);
                        }

                    }
                    else if (MatchVariantsToGenotypeAndInheritance.Status.isPresumedCarrier(
                            rvcf.getSampleStatus().get(sample)))
                    {
                        if (!sampleGeneCombo.contains(gene + "_" + sample))
                        {
                            int count = geneToCarrier.get(gene);
                            geneToCarrier.put(gene, count + 1);
                            sampleGeneCombo.add(gene + "_" + sample);
                        }
                    }
                    else
                    {
                        throw new Exception("ERROR: Unknown sample Status: " + rvcf.getSampleStatus().get(sample));
                    }
                }
            }

        }

        pw.println(HEADER);
        for(Map.Entry<String,Integer> geneToAffectedEntry : geneToAffected.entrySet())
        {
            String gene = geneToAffectedEntry.getKey();
            pw.println(gene + "\t" + geneToAffectedEntry.getValue() + "\t" + geneToCarrier.get(gene)+ "\t" + (((double)geneToAffectedEntry.getValue())/((double)nrOfSamples)) + "\t" + (((double)geneToCarrier.get(gene))/((double)nrOfSamples)));
        }

        pw.flush();
        pw.close();

    }

}
