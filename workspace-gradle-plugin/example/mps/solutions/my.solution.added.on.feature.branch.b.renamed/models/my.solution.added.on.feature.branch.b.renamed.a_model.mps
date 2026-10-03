<?xml version="1.0" encoding="UTF-8"?>
<model ref="r:510951b7-cc98-46d5-92a4-46d3620565ae(my.solution.added.on.feature.branch.b.renamed.a_model)">
  <persistence version="9" />
  <languages>
    <devkit ref="fbc25dd2-5da4-483a-8b19-70928e1b62d7(jetbrains.mps.devkit.general-purpose)" />
  </languages>
  <imports />
  <registry>
    <language id="f3061a53-9226-4cc5-a443-f952ceaf5816" name="jetbrains.mps.baseLanguage">
      <concept id="1068390468198" name="jetbrains.mps.baseLanguage.structure.ClassConcept" flags="ig" index="312cEu" />
      <concept id="1068580123132" name="jetbrains.mps.baseLanguage.structure.BaseMethodDeclaration" flags="ng" index="3clF44">
        <child id="1068580123133" name="returnType" index="3clF45" />
        <child id="1068580123135" name="body" index="3clF47" />
      </concept>
      <concept id="1068580123165" name="jetbrains.mps.baseLanguage.structure.InstanceMethodDeclaration" flags="ig" index="3clFb_" />
      <concept id="1068580123136" name="jetbrains.mps.baseLanguage.structure.StatementList" flags="sn" stub="5293379017992965193" index="3clFbS" />
      <concept id="1068581517677" name="jetbrains.mps.baseLanguage.structure.VoidType" flags="in" index="3cqZAl" />
      <concept id="1107461130800" name="jetbrains.mps.baseLanguage.structure.Classifier" flags="ng" index="3pOWGL">
        <child id="5375687026011219971" name="member" index="jymVt" unordered="true" />
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
  <node concept="312cEu" id="kw_$T6$Ikt">
    <property role="TrG5h" value="NewRootNodeOnFeatureBranchB" />
    <node concept="3clFb_" id="kw_$T6$Ilv" role="jymVt">
      <property role="TrG5h" value="abc" />
      <node concept="3cqZAl" id="kw_$T6$Ilx" role="3clF45" />
      <node concept="3Tm1VV" id="kw_$T6$Ily" role="1B3o_S" />
      <node concept="3clFbS" id="kw_$T6$Ilz" role="3clF47" />
    </node>
    <node concept="3clFb_" id="kw_$T6$Ina" role="jymVt">
      <property role="TrG5h" value="def" />
      <node concept="3cqZAl" id="kw_$T6$Inc" role="3clF45" />
      <node concept="3Tm1VV" id="kw_$T6$Ind" role="1B3o_S" />
      <node concept="3clFbS" id="kw_$T6$Ine" role="3clF47" />
    </node>
    <node concept="3Tm1VV" id="kw_$T6$Iku" role="1B3o_S" />
  </node>
</model>

