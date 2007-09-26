
/**
 *  StefanC
 */
package com.vectrace.MercurialEclipse.dialogs;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

final class CommitResourceLabelProvider extends LabelProvider implements ITableLabelProvider
{

  public Image getColumnImage(Object element, int columnIndex)
  {
    // No images.
    return null;
  }

  public String getColumnText(Object element, int columnIndex)
  {
    if ((element instanceof CommitResource) != true)
    {
      return "Type Error";
    }
    CommitResource resource = (CommitResource) element;

    switch (columnIndex)
    {
    case 0:
      return "";
    case 1:
      return resource.getPath().toString();
    case 2:
      return resource.getStatus();
    default:
      return "Col Error: " + columnIndex;
    }
  }
}