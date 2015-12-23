/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hits.nlp.ontonotes2mmax;

import java.io.File;
import java.io.FileFilter;

/**
 *
 * @author caije
 */
public class ONFFilter implements FileFilter
{
    public boolean accept(File file)
    { return file.toString().endsWith(".onf"); }

}
