
| Default VTX Table                                                       |
|-------------------------------------------------------------------------|
| vtxtable band 1 BAND_A A CUSTOM 5865 5845 5825 5805 5875 5865 5745 5725 |
| vtxtable band 2 BAND_B B CUSTOM 5733 5752 5771 5790 5809 5828 5857 5866 |
| vtxtable band 3 BAND_E E CUSTOM 5705 5685 5665 5885 5905 5905 5905 5905 |
| vtxtable band 4 BAND_F F CUSTOM 5740 5760 5780 5800 5820 5840 5860 5880 |
| vtxtable band 5 BAND_R R CUSTOM 5658 5695 5732 5769 5806 5843 5880 5917 |

This can also be used to switch to specific band/channels based on a switch but since the band/channel in the example above are 0 it won't change channels - only power. See the help for the vtx command.  
  
`vtx` - vtx channels on switch  
`<index> <aux_channel> <vtx_band> <vtx_channel> <vtx_power> <start_range> <end_range>`
```
vtx 0 7 0 0 1 900 1199
vtx 1 7 0 0 2 1200 1399
vtx 2 7 0 0 3 1400 1599
vtx 3 7 0 0 4 1599 2100
```

