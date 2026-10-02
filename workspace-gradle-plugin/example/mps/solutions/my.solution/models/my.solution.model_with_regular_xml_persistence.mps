<?xml version="1.0" encoding="UTF-8"?>
<model ref="r:92aca1fe-8b89-4dfc-89d8-57125a38c96f(my.solution.model_with_regular_xml_persistence)">
  <persistence version="9" />
  <languages>
    <use id="95dcc83d-7fad-44e8-9994-1ed13a0c7a59" name="my.language" version="0" />
    <devkit ref="fbc25dd2-5da4-483a-8b19-70928e1b62d7(jetbrains.mps.devkit.general-purpose)" />
  </languages>
  <imports>
    <import index="ng9r" ref="r:510951b7-cc98-46d5-92a4-46d3620565ae(my.solution.added.on.feature.branch.b.renamed.a_model)" />
  </imports>
  <registry>
    <language id="f3061a53-9226-4cc5-a443-f952ceaf5816" name="jetbrains.mps.baseLanguage">
      <concept id="1068390468198" name="jetbrains.mps.baseLanguage.structure.ClassConcept" flags="ig" index="312cEu">
        <child id="1165602531693" name="superclass" index="1zkMxy" />
      </concept>
      <concept id="1107535904670" name="jetbrains.mps.baseLanguage.structure.ClassifierType" flags="in" index="3uibUv">
        <reference id="1107535924139" name="classifier" index="3uigEE" />
      </concept>
      <concept id="1178549954367" name="jetbrains.mps.baseLanguage.structure.IVisible" flags="ngI" index="1B3ioH">
        <child id="1178549979242" name="visibility" index="1B3o_S" />
      </concept>
      <concept id="1146644602865" name="jetbrains.mps.baseLanguage.structure.PublicVisibility" flags="nn" index="3Tm1VV" />
    </language>
    <language id="ceab5195-25ea-4f22-9b92-103b95ca8c0c" name="jetbrains.mps.lang.core">
      <concept id="1169194658468" name="jetbrains.mps.lang.core.structure.INamedConcept" flags="ngI" index="TrEIO">
        <property id="1169194664001" name="name" index="TrG5h" />
      </concept>
    </language>
  </registry>
  <node concept="312cEu" id="kw_$T6$I9F">
    <property role="TrG5h" value="AddedRootToRegularPersistence" />
    <node concept="3Tm1VV" id="kw_$T6$I9G" role="1B3o_S" />
    <node concept="3uibUv" id="kw_$T6$IrO" role="1zkMxy">
      <ref role="3uigEE" to="ng9r:kw_$T6$Ikt" resolve="NewRootNodeOnFeatureBranchB" />
    </node>
  </node>
</model>

